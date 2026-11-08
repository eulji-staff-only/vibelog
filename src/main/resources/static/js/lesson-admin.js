/*
 * 백오피스 수업 편집 화면(A-06)의 활동 탭 동작 (API-ADM-21~25, FR-LSN-011, #101).
 * 모달 열고 닫기·탭 전환은 공통 ui.js가 한다. 여기서는 이 화면만의 동작만 담당한다.
 *
 * - 활동 순서 드래그: data-activity-reorder 안의 <li draggable> 를 드래그해 자리를 바꾸고,
 *   "순서 저장" 버튼(별도 폼 activity-order-form)을 누르면 현재 DOM 순서로 activityIds 히든 입력을
 *   만들어 함께 제출한다 (서버가 PL-LSN-001을 검사해 INVALID_ORDER면 오류 화면으로 보낸다).
 * - 개념 이미지 올리기: 숨긴 file input을 열어 고른 파일을 /api/admin/uploads/images 로 올리고,
 *   받은 url을 마크다운 이미지 문법으로 본문 textarea 끝에 넣는다 (편집기가 textarea뿐이라 미리보기는 없다).
 * - 수업 비공개(API-ADM-33, FR-LSN-014, #118): 비공개 버튼을 누르면 확인 모달이 열리면서(ui.js) 동시에
 *   영향 학습자 수(API-ADM-32)를 불러와 모달 본문 문구를 바꾼다. 실제 비공개 처리는 모달 안 폼이 한다.
 */
(function () {
    'use strict';

    function initActivityReorder() {
        document.querySelectorAll('[data-activity-reorder]').forEach((list) => {
            let dragging = null;

            list.querySelectorAll('li[draggable="true"]').forEach((row) => {
                row.addEventListener('dragstart', () => {
                    dragging = row;
                    row.classList.add('is-dragging');
                });
                row.addEventListener('dragend', () => {
                    row.classList.remove('is-dragging');
                    dragging = null;
                });
                row.addEventListener('dragover', (e) => {
                    e.preventDefault();
                    if (!dragging || dragging === row) return;
                    const rect = row.getBoundingClientRect();
                    const putAfter = (e.clientY - rect.top) > rect.height / 2;
                    list.insertBefore(dragging, putAfter ? row.nextSibling : row);
                });
            });
        });
    }

    function initActivityOrderSave() {
        const form = document.getElementById('activity-order-form');
        const list = document.querySelector('[data-activity-reorder]');
        if (!form || !list) return;

        // 폼 자체는 서버가 렌더링한 것(th:action)이라 CSRF 토큰 히든 입력이 이미 들어 있다.
        // 제출 직전에 activityIds 만 지우고 현재 드래그 순서로 다시 넣는다.
        form.addEventListener('submit', () => {
            form.querySelectorAll('input[name="activityIds"]').forEach((el) => el.remove());
            list.querySelectorAll('li[data-activity-id]').forEach((row) => {
                const input = document.createElement('input');
                input.type = 'hidden';
                input.name = 'activityIds';
                input.value = row.dataset.activityId;
                form.appendChild(input);
            });
        });
    }

    function initConceptImageUpload() {
        document.querySelectorAll('[data-image-upload-trigger]').forEach((button) => {
            button.addEventListener('click', () => {
                const input = document.getElementById(button.dataset.imageUploadTrigger);
                if (input) input.click();
            });
        });

        document.querySelectorAll('input[type="file"][data-image-upload-target]').forEach((input) => {
            input.addEventListener('change', async () => {
                const file = input.files[0];
                if (!file) return;
                const textarea = document.getElementById(input.dataset.imageUploadTarget);
                const token = document.querySelector('meta[name="_csrf"]').content;
                const header = document.querySelector('meta[name="_csrf_header"]').content;
                const formData = new FormData();
                formData.append('image', file);

                try {
                    const response = await fetch('/api/admin/uploads/images', {
                        method: 'POST',
                        headers: { [header]: token },
                        body: formData,
                    });
                    const body = await response.json().catch(() => null);
                    if (response.ok && body && textarea) {
                        textarea.value += (textarea.value.endsWith('\n') || textarea.value === '' ? '' : '\n')
                            + '![](' + body.url + ')\n';
                    } else {
                        alert(body?.message ?? 'JPG·PNG·WebP 파일만, 5MB까지 올릴 수 있습니다.');
                    }
                } catch (e) {
                    alert('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
                } finally {
                    input.value = '';
                }
            });
        });
    }

    function initLessonUnpublishImpact() {
        const trigger = document.getElementById('lesson-unpublish-trigger');
        const body = document.getElementById('lesson-unpublish-modal-body');
        if (!trigger || !body) return;

        trigger.addEventListener('click', async () => {
            body.textContent = '불러오는 중입니다...';
            try {
                const response = await fetch('/api/admin/lessons/' + trigger.dataset.lessonId + '/impact');
                if (!response.ok) throw new Error('impact request failed');
                const result = await response.json();
                body.textContent = result.activeLearnerCount > 0
                    ? '현재 ' + result.activeLearnerCount + '명이 이 코스를 수강 중입니다. 비공개해도 될까요?'
                    : '이 수업을 비공개할까요?';
            } catch (e) {
                body.textContent = '영향 받는 학습자 수를 확인하지 못했습니다. 그래도 비공개할까요?';
            }
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        initActivityReorder();
        initActivityOrderSave();
        initConceptImageUpload();
        initLessonUnpublishImpact();
    });
})();

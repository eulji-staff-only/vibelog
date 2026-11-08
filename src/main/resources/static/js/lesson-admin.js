/*
 * 백오피스 수업 편집 화면(A-06)의 활동 탭 동작 (API-ADM-21~25, FR-LSN-011, #101).
 * 모달 열고 닫기·탭 전환은 공통 ui.js가 한다. 여기서는 이 화면만의 동작만 담당한다.
 *
 * - 활동 순서 드래그: data-activity-reorder 안의 <li draggable> 를 드래그해 자리를 바꾸고,
 *   "순서 저장" 버튼(별도 폼 activity-order-form)을 누르면 현재 DOM 순서로 activityIds 히든 입력을
 *   만들어 함께 제출한다 (서버가 PL-LSN-001을 검사해 INVALID_ORDER면 오류 화면으로 보낸다).
 * - 개념 이미지 올리기: 숨긴 file input을 열어 고른 파일을 /api/admin/uploads/images 로 올리고,
 *   받은 url을 마크다운 이미지 문법으로 본문 textarea 끝에 넣는다 (편집기가 textarea뿐이라 미리보기는 없다).
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

    document.addEventListener('DOMContentLoaded', () => {
        initActivityReorder();
        initActivityOrderSave();
        initConceptImageUpload();
    });
})();

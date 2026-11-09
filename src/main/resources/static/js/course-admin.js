/*
 * course-admin.js — 운영자 트랙 관리(A-02)·카테고리 관리(A-03) 화면 동작 (#95).
 *
 * - "만들기" 버튼: 모달은 공통 ui.js 가 연다. 수정 화면(?trackId=)에서 눌렀으면 폼을 "만들기" 상태로 비운다.
 * - 삭제: 각 줄의 삭제 폼을 바로 보내지 않고 확인 모달(설계서 5-1 "삭제 → 확인 모달")을 거친다.
 * - 카테고리 순서: 줄을 끌어 놓거나 ↑·↓ 버튼을 누르면 새 순서를 POST /api/admin/categories/order 로 바로 저장한다
 *   (API-ADM-10, JSON, CSRF 헤더). 실패하면 오류 토스트를 띄우고 화면을 새로 고쳐 저장된 순서로 되돌린다.
 */
(function () {
    'use strict';

    /* ---- 만들기 모달: 수정 폼이 열려 있던 화면에서도 빈 "만들기" 폼으로 ---- */
    document.querySelectorAll('[data-ca-create]').forEach((button) => {
        button.addEventListener('click', () => {
            const modal = document.getElementById(button.getAttribute('data-ui-modal-open'));
            const form = modal && modal.querySelector('form[data-create-action]');
            if (!form || form.getAttribute('action') === form.dataset.createAction) return;
            form.setAttribute('action', form.dataset.createAction);
            form.querySelectorAll('input:not([type="hidden"]), textarea').forEach((field) => {
                field.value = '';
                field.removeAttribute('aria-invalid');
            });
            form.querySelectorAll('.ca-error').forEach((error) => error.remove());
            modal.querySelector('.vl-ui-modal__title').textContent = form.dataset.createTitle;
            form.querySelector('button[type="submit"]').textContent = '만들기';
        });
    });

    /* ---- 삭제 확인 모달 ---- */
    const deleteModal = document.getElementById('ca-delete-modal');
    let pendingDelete = null;
    document.querySelectorAll('form[data-ca-delete]').forEach((form) => {
        form.addEventListener('submit', (e) => {
            if (form === pendingDelete) return;   // 확인을 눌러 보내는 중
            e.preventDefault();
            pendingDelete = form;
            deleteModal.querySelector('.vl-ui-modal__title').textContent = '"' + form.dataset.caDelete + '"을(를) 삭제할까요?';
            deleteModal.hidden = false;
            deleteModal.querySelector('.vl-ui-modal__title').focus();
        });
    });
    if (deleteModal) {
        deleteModal.querySelector('.vl-ui-btn.is-navy').addEventListener('click', () => {
            if (pendingDelete) pendingDelete.requestSubmit();
        });
    }

    /* ---- 카테고리 순서 (드래그·↑↓) ---- */
    const list = document.querySelector('[data-ca-order]');
    if (!list) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    async function saveOrder() {
        const ids = Array.from(list.querySelectorAll('[data-id]')).map((row) => Number(row.dataset.id));
        try {
            const response = await fetch('/api/admin/categories/order', {
                method: 'POST',
                headers: { [csrfHeader]: csrfToken, 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify({ courseCategoryIds: ids })
            });
            if (!response.ok) {
                const data = await response.json().catch(() => ({}));
                throw new Error(data.message || '잠시 후 다시 시도해 주세요.');
            }
            if (window.VLToast) window.VLToast.show('순서를 저장했습니다');
        } catch (error) {
            if (window.VLToast) window.VLToast.show(error.message, { variant: 'danger', actionLabel: '다시 불러오기', onAction: () => location.reload() });
        }
    }

    let dragging = null;
    list.querySelectorAll('li[draggable="true"]').forEach((row) => {
        row.addEventListener('dragstart', () => {
            dragging = row;
            row.classList.add('is-dragging');
        });
        row.addEventListener('dragend', () => {
            row.classList.remove('is-dragging');
            if (dragging) saveOrder();
            dragging = null;
        });
        row.addEventListener('dragover', (e) => {
            e.preventDefault();
            if (!dragging || dragging === row) return;
            const rect = row.getBoundingClientRect();
            const putAfter = e.clientY > rect.top + rect.height / 2;
            list.insertBefore(dragging, putAfter ? row.nextSibling : row);
        });
    });

    // 키보드로도 순서를 바꿀 수 있게 ↑·↓ 버튼 (드래그는 마우스만 된다)
    list.querySelectorAll('[data-ca-move]').forEach((button) => {
        button.addEventListener('click', () => {
            const row = button.closest('li');
            const target = button.dataset.caMove === 'up' ? row.previousElementSibling : row.nextElementSibling;
            if (!target) return;
            list.insertBefore(row, button.dataset.caMove === 'up' ? target : target.nextSibling);
            button.focus();
            saveOrder();
        });
    });
})();

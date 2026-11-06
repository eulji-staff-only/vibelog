/*
 * ui.js — 공통 UI 조각(#46)이 화면 안에서 혼자 여닫는 동작만 담당한다.
 * 서버를 부르지 않는다 (확인·토스트의 실제 저장·삭제·다시 시도 동작은 ③ 단계에서 각 화면이 연결).
 *
 * - 모달: data-ui-modal-open="<id>" 버튼 → data-ui-modal="<id>" 를 열고, 그 안 data-ui-modal-close,
 *   바깥(배경) 클릭, Esc 키로 닫는다. 닫으면 열었던 버튼으로 포커스를 되돌린다 (5-2 키보드 포커스).
 * - 탭: data-ui-tabs 안의 [role="tab"] 버튼을 누르면 aria-controls 로 연결된 패널만 보여준다.
 * - 토스트: VLToast.show(message, options) 로 띄운다. options = { variant: 'info'|'danger',
 *   duration(ms), actionLabel, onAction }. variant 'danger' 는 "다시 시도" 버튼과 함께 닫을 때까지
 *   유지하고(5-3), 그 밖에는 기본 3초 뒤 사라진다.
 * - data-ui-back / data-ui-reload: 오류 화면(E-01~E-03)의 "이전 페이지로" · "다시 시도" 버튼.
 */
(function () {
    'use strict';

    /* ---- 확인 모달 ---- */
    function initModals() {
        let lastTrigger = null;

        const openModal = (id, trigger) => {
            const modal = document.getElementById(id);
            if (!modal) return;
            lastTrigger = trigger || null;
            modal.hidden = false;
            const title = modal.querySelector('.vl-ui-modal__title');
            if (title) title.focus();
        };

        const closeModal = (modal) => {
            modal.hidden = true;
            if (lastTrigger) lastTrigger.focus();
        };

        document.addEventListener('click', (e) => {
            const opener = e.target.closest('[data-ui-modal-open]');
            if (opener) {
                openModal(opener.getAttribute('data-ui-modal-open'), opener);
                return;
            }
            const closer = e.target.closest('[data-ui-modal-close]');
            if (closer) {
                const modal = closer.closest('[data-ui-modal]');
                if (modal) closeModal(modal);
                return;
            }
            // 모달 바깥(어두운 배경) 클릭
            if (e.target.matches('[data-ui-modal]')) {
                closeModal(e.target);
            }
        });

        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            const openModalEl = document.querySelector('[data-ui-modal]:not([hidden])');
            if (openModalEl) closeModal(openModalEl);
        });
    }

    /* ---- 탭 ---- */
    function initTabs() {
        document.querySelectorAll('[data-ui-tabs]').forEach((group) => {
            const tabs = [...group.querySelectorAll('[role="tab"]')];

            const select = (tab) => {
                tabs.forEach((t) => t.setAttribute('aria-selected', String(t === tab)));
                tabs.forEach((t) => {
                    const panel = document.getElementById(t.getAttribute('aria-controls'));
                    if (panel) panel.hidden = t !== tab;
                });
            };

            tabs.forEach((tab) => tab.addEventListener('click', () => select(tab)));
        });
    }

    /* ---- 토스트 ---- */
    function showToast(message, options) {
        const opts = options || {};
        const container = document.querySelector('[data-ui-toasts]');
        if (!container) {
            // 화면에 토스트 컨테이너(common/ui :: toastContainer)를 아직 안 넣었다는 뜻
            return;
        }

        const toast = document.createElement('div');
        toast.className = 'vl-ui-toast' + (opts.variant === 'danger' ? ' is-danger' : '');
        toast.setAttribute('role', 'status');

        const text = document.createElement('span');
        text.className = 'vl-ui-toast__message';
        text.textContent = message;
        toast.appendChild(text);

        const remove = () => toast.remove();

        if (opts.actionLabel) {
            const action = document.createElement('button');
            action.type = 'button';
            action.className = 'vl-ui-toast__action';
            action.textContent = opts.actionLabel;
            action.addEventListener('click', () => {
                if (typeof opts.onAction === 'function') opts.onAction();
                remove();
            });
            toast.appendChild(action);
        }

        const close = document.createElement('button');
        close.type = 'button';
        close.className = 'vl-ui-toast__close';
        close.setAttribute('aria-label', '닫기');
        close.textContent = '×';
        close.addEventListener('click', remove);
        toast.appendChild(close);

        container.appendChild(toast);

        // 5-3: 오류 토스트는 닫을 때까지 유지, 그 밖은 기본 3초 뒤 사라짐
        if (opts.variant !== 'danger') {
            setTimeout(remove, opts.duration || 3000);
        }
    }

    window.VLToast = { show: showToast };

    /* ---- 오류 화면: 이전 페이지로 · 다시 시도 ---- */
    function initPageActions() {
        document.addEventListener('click', (e) => {
            if (e.target.closest('[data-ui-back]')) {
                e.preventDefault();
                history.back();
            }
            if (e.target.closest('[data-ui-reload]')) {
                e.preventDefault();
                location.reload();
            }
        });
    }

    document.addEventListener('DOMContentLoaded', () => {
        initModals();
        initTabs();
        initPageActions();
    });
})();

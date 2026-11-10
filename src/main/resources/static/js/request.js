/*
 * request.js — 모든 화면이 서버를 부를 때의 공통 동작 (#367). 공통 헤더(common/layout.html)와 헤더 없는 수업 화면이 불러온다.
 * defer 없이 불러 다른 화면 JS 보다 먼저 fetch 를 감싼다.
 *
 * - 조회 1회 자동 재시도 (PL-ERR-002): GET fetch 가 네트워크 오류이거나 5xx 면 한 번 더 보낸다.
 *   저장·제출(GET 이 아닌 요청)은 다시 보내지 않는다. 4xx 는 다시 보내도 같아서 그대로 돌려준다.
 * - 폼 중복 제출 막기 (PL-ERR-003, 5-3): POST 폼을 보내면 페이지가 바뀔 때까지 같은 폼을 다시 보내지 않고,
 *   제출 버튼에 aria-disabled 와 스피너(.is-loading)를 붙인다. disabled 를 쓰지 않는 이유: 누른 버튼의 name·value 가 빠진다.
 *   화면 JS 가 직접 처리하는 제출(preventDefault 한 것, fetch 로 보내는 모달 등)은 건드리지 않는다.
 *   window 에서 듣기 때문에 폼·document 에 단 화면 JS 의 submit 처리보다 늦게 돌아 preventDefault 여부를 알 수 있다.
 */
(() => {
    const nativeFetch = window.fetch.bind(window);
    window.fetch = async (input, init) => {
        const method = (init?.method ?? (input instanceof Request ? input.method : 'GET')).toUpperCase();
        if (method !== 'GET') return nativeFetch(input, init);
        try {
            const response = await nativeFetch(input, init);
            if (response.status < 500) return response;
        } catch (e) {
            if (e.name === 'AbortError') throw e;   // 화면이 취소한 요청은 다시 보내지 않는다
        }
        return nativeFetch(input, init);
    };

    const submitButtons = (form) => [...form.elements].filter((el) => el.type === 'submit');

    window.addEventListener('submit', (e) => {
        const form = e.target;
        if (e.defaultPrevented || form.method !== 'post') return;
        if (form.dataset.submitting) {
            e.preventDefault();
            return;
        }
        form.dataset.submitting = 'true';
        submitButtons(form).forEach((button) => {
            button.setAttribute('aria-disabled', 'true');
            button.classList.add('is-loading');
        });
    });

    // 뒤로 가기로 복원된 화면(bfcache)은 다시 보낼 수 있게 푼다
    window.addEventListener('pageshow', (e) => {
        if (!e.persisted) return;
        document.querySelectorAll('form[data-submitting]').forEach((form) => {
            delete form.dataset.submitting;
            submitButtons(form).filter((button) => button.classList.contains('is-loading')).forEach((button) => {
                button.removeAttribute('aria-disabled');
                button.classList.remove('is-loading');
            });
        });
    });
})();

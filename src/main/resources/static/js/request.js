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
 * - 작성 중 내용 보관 (PL-ERR-005, PL-ERR-004 7일, #366): data-draft="이름" 폼(글쓰기·댓글·답글·쪽지)의 글자 칸과 고른 라디오를
 *   입력할 때마다 회원별 키(vl.draft.{회원 ID}.{이름})로 보관하고, 다시 열면 채운다. 세션이 끊겨 로그인하고 돌아와도 남아 있다.
 *   저장에 성공하면 Controller 가 flash 로 넣은 data-draft-clear 표시를 보고 지운다. 포트폴리오(portfolio.js)와 같은 방식이다.
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

    const DRAFT_MAX_AGE = 7 * 24 * 60 * 60 * 1000;
    document.addEventListener('DOMContentLoaded', () => {
        const owner = document.querySelector('[data-draft-owner]')?.dataset.draftOwner;
        if (!owner) return;   // 비회원: 작성 폼이 없다
        const keyOf = (name) => `vl.draft.${owner}.${name}`;
        const storage = {
            read: (key) => { try { return JSON.parse(localStorage.getItem(key)); } catch (e) { return null; } },
            write: (key, value) => { try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* 사용 불가·가득 참 */ } },
            remove: (key) => { try { localStorage.removeItem(key); } catch (e) { /* 사용 불가 */ } },
        };
        document.querySelectorAll('[data-draft-clear]').forEach((el) => storage.remove(keyOf(el.dataset.draftClear)));

        document.querySelectorAll('form[data-draft]').forEach((form) => {
            const key = keyOf(form.dataset.draft);
            const fields = () => [...form.elements].filter((el) => el.name
                && (el.tagName === 'TEXTAREA' || ['text', 'url', 'radio'].includes(el.type)));
            const draft = storage.read(key);
            if (draft && Date.now() - draft.savedAt <= DRAFT_MAX_AGE) {
                fields().forEach((el) => {
                    if (!(el.name in draft.values)) return;
                    if (el.type === 'radio') el.checked = el.value === draft.values[el.name];
                    else el.value = draft.values[el.name];
                });
                window.VLToast?.show('작성 중이던 내용을 불러왔습니다');
            } else if (draft) {
                storage.remove(key);   // 7일이 지났다
            }
            form.addEventListener('input', () => {
                const values = {};
                fields().forEach((el) => { if (el.type !== 'radio' || el.checked) values[el.name] = el.value; });
                storage.write(key, { savedAt: Date.now(), values });
            });
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

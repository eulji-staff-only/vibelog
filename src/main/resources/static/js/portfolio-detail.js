/*
 * 포트폴리오 상세(U-20): 받은 리뷰의 "도움됨" 켜고 끄기 (API-REV-06, JSON). 작품 주인에게만 버튼이 있다.
 * 리뷰 요청 확인 모달(M-08)은 공통 ui.js 가 열고 닫는다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const buttons = document.querySelectorAll('[data-helpful]');
    if (!buttons.length) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    buttons.forEach((button) => {
        button.addEventListener('click', () => {
            if (button.disabled) return;
            button.disabled = true;   // 두 번 눌러 두 번 보내지 않게 (PL-ERR-003)
            fetch(button.dataset.helpfulUrl, { method: 'POST', headers: { [csrfHeader]: csrfToken } })
                .then((res) => res.json().then((body) => {
                    if (!res.ok) throw body;
                    return body;
                }))
                .then(({ helpful }) => {
                    button.setAttribute('aria-pressed', String(helpful));
                    button.textContent = helpful ? '도움됨 ✓' : '도움됨';
                })
                .catch((err) => {
                    // 네트워크 끊김 같은 브라우저 오류(Error)는 기술 문구라 보여 주지 않는다 (PL-ERR-001)
                    const message = err instanceof Error || !err.message ? '바꾸지 못했습니다. 잠시 후 다시 시도해 주세요.' : err.message;
                    if (window.VLToast) window.VLToast.show(message, { variant: 'danger' });
                })
                .finally(() => { button.disabled = false; });
        });
    });
});

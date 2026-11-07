/*
 * 이메일 인증 화면(U-04)의 "인증 메일 다시 받기" (API-USR-10, #89).
 * JSON POST 라서 CSRF 토큰을 헤더로 보낸다. 보내면 1분 동안 버튼을 막는다 (PL-CST-002).
 * 실패하면 서버가 준 {code, message} 의 message 를 그대로 보여 준다 (RESEND_TOO_SOON·ALREADY_VERIFIED·INVALID_INPUT).
 */
(() => {
    const box = document.querySelector('[data-resend]');
    if (!box) return;

    const button = box.querySelector('[data-resend-button]');
    const email = box.querySelector('[data-resend-email]');
    const done = box.querySelector('[data-resend-done]');
    const error = box.querySelector('[data-resend-error]');
    const wait = box.querySelector('[data-resend-wait]');
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    const showError = (message) => {
        error.textContent = message;
        error.hidden = false;
    };

    button.addEventListener('click', async () => {
        error.hidden = true;
        if (email && !email.value.trim()) {
            showError('이메일을 입력해 주세요');
            return;
        }
        button.disabled = true;
        try {
            const response = await fetch('/api/auth/verification-mail', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify(email ? { email: email.value.trim() } : {}),
            });
            if (response.ok) {
                done.hidden = false;
                wait.hidden = false;
                setTimeout(() => { button.disabled = false; wait.hidden = true; }, 60_000);
                return;
            }
            const body = await response.json().catch(() => null);
            showError(body?.message ?? '요청을 처리하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.');
        } catch (e) {
            showError('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
        }
        button.disabled = false;
    });
})();

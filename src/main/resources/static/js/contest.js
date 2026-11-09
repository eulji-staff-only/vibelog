/*
 * contest.js — U-32 투표 (API-CTS-03, JSON POST + CSRF 헤더). 출품 확인(M-09)은 ui.js 공통 모달이 여닫는다.
 * 투표하면 그 카드에 "투표함"을 보여 주고, 하루 1표(PL-CTS-003)라 다른 투표 버튼을 모두 막는다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const buttons = [...document.querySelectorAll('[data-vote]')];

    const closeToday = () => {
        buttons.forEach((b) => { b.disabled = true; });
        const done = document.querySelector('[data-voted-today]');
        if (done) done.hidden = false;
    };

    buttons.forEach((button) => {
        button.addEventListener('click', async () => {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            button.disabled = true;
            try {
                const response = await fetch(button.dataset.url, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', [header]: token },
                    body: JSON.stringify({ contestEntryId: Number(button.dataset.entryId) }),
                });
                // 실패는 {code, message} JSON. 세션이 끝나 로그인 화면(HTML)이 오면 읽지 못해 null
                const body = await response.json().catch(() => null);
                if (response.ok && body) {
                    const badge = document.createElement('span');
                    badge.className = 'vl-ui-badge is-success';
                    badge.textContent = '투표함';
                    button.replaceWith(badge);
                    closeToday();
                    VLToast.show('투표했습니다');
                    return;
                }
                if (body?.code === 'DAILY_VOTE_LIMIT') closeToday();
                VLToast.show(body?.message ?? '투표하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.', { variant: 'danger' });
            } catch (e) {
                VLToast.show('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.', { variant: 'danger' });
            }
            if (button.isConnected && !document.querySelector('[data-voted-today]:not([hidden])')) {
                button.disabled = false;
            }
        });
    });
});

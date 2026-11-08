/*
 * 커뮤니티 화면(U-22·U-23) 동작: 삭제 확인 모달(M-05) 열기, 제목·본문 글자 수 표시 (PL-CMT-010),
 * 즐겨찾기·팔로우 켜고 끄기 (API-CMT-13·15, JSON POST + CSRF 헤더). 모달 닫기는 dialog 안 form method="dialog" 버튼이 한다.
 * 공통 UI 조각(#46)의 모달이 머지되면 그것으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-open-dialog]').forEach((button) => {
        button.addEventListener('click', () => document.getElementById(button.dataset.openDialog).showModal());
    });

    document.querySelectorAll('[data-count]').forEach((input) => {
        const count = document.querySelector(`[data-count-for="${input.id}"]`);
        const render = () => { count.textContent = input.value.length.toLocaleString(); };
        input.addEventListener('input', render);
        render();
    });

    document.querySelectorAll('[data-follow]').forEach((button) => {
        button.addEventListener('click', async () => {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            button.disabled = true;
            try {
                const response = await fetch(button.dataset.url, { method: 'POST', headers: { [header]: token } });
                if (response.ok) {
                    const { following, followerCount } = await response.json();
                    button.setAttribute('aria-pressed', String(following));
                    button.textContent = following ? '팔로우 취소' : '팔로우';
                    document.querySelector('[data-follower-count]').textContent = followerCount;
                }
            } finally {
                button.disabled = false;
            }
        });
    });

    document.querySelectorAll('[data-favorite]').forEach((button) => {
        button.addEventListener('click', async () => {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            button.disabled = true;
            try {
                const response = await fetch(button.dataset.url, { method: 'POST', headers: { [header]: token } });
                if (response.ok) {
                    const { favorited } = await response.json();
                    button.setAttribute('aria-pressed', String(favorited));
                    button.textContent = favorited ? '즐겨찾기 취소' : '즐겨찾기';
                }
            } finally {
                button.disabled = false;
            }
        });
    });
});

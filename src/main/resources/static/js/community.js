/*
 * 커뮤니티 화면(U-22·U-23) 동작: 삭제 확인 모달(M-05) 열기, 제목·본문 글자 수 표시 (PL-CMT-010),
 * 즐겨찾기·팔로우·추천 켜고 끄기 (API-CMT-13·15·11, JSON POST + CSRF 헤더). 모달 닫기는 dialog 안 form method="dialog" 버튼이 한다.
 * 공통 UI 조각(#46)의 모달이 머지되면 그것으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-open-dialog]').forEach((button) => {
        button.addEventListener('click', () => document.getElementById(button.dataset.openDialog).showModal());
    });

    // 글 수정: 기존 사진 위 X 를 누르면 그 사진을 지운다 (keepPostImageIds 체크를 풀고 숨김, API-CMT-06)
    document.querySelectorAll('[data-keep-remove]').forEach((button) => {
        button.addEventListener('click', () => {
            const item = button.closest('[data-keep-item]');
            item.querySelector('input[name="keepPostImageIds"]').checked = false;
            item.hidden = true;
            document.getElementById('images')?.focus();
        });
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
                const body = await response.json().catch(() => null);
                if (response.ok && body) {
                    button.setAttribute('aria-pressed', String(body.following));
                    button.textContent = body.following ? '팔로우 취소' : '팔로우';
                    document.querySelector('[data-follower-count]').textContent = body.followerCount;
                } else {
                    alert(body?.message ?? '팔로우를 처리하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.');
                }
            } catch (e) {
                alert('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
            } finally {
                button.disabled = false;
            }
        });
    });

    // 추천 (작품 공유 글, API-CMT-11). 추천 수도 함께 바꾼다
    document.querySelectorAll('[data-recommend]').forEach((button) => {
        button.addEventListener('click', async () => {
            const token = document.querySelector('meta[name="_csrf"]').content;
            const header = document.querySelector('meta[name="_csrf_header"]').content;
            button.disabled = true;
            try {
                const response = await fetch(button.dataset.url, { method: 'POST', headers: { [header]: token } });
                const body = await response.json().catch(() => null);
                if (response.ok && body) {
                    button.setAttribute('aria-pressed', String(body.recommended));
                    button.textContent = body.recommended ? '추천 취소' : '추천';
                    document.querySelector('[data-recommend-count]').textContent = `추천 ${body.recommendCount}`;
                } else {
                    alert(body?.message ?? '추천을 처리하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.');
                }
            } catch (e) {
                alert('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
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
                // 실패는 {code, message} JSON. 세션이 끝나 로그인 화면(HTML)이 오면 읽지 못해 null
                const body = await response.json().catch(() => null);
                if (response.ok && body) {
                    button.setAttribute('aria-pressed', String(body.favorited));
                    button.textContent = body.favorited ? '즐겨찾기 취소' : '즐겨찾기';
                } else {
                    alert(body?.message ?? '즐겨찾기를 처리하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.');
                }
            } catch (e) {
                alert('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
            } finally {
                button.disabled = false;
            }
        });
    });
});

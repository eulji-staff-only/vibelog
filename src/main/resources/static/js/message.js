/*
 * 쪽지함(U-27) 새 쪽지: 받는 사람을 닉네임 앞부분으로 찾아 고른다 (API-MSG-02, #184).
 * 입력이 멈추고 250ms 뒤에 /api/users 를 부르고, 고르면 숨은 칸 receiverMemberId 에 회원 ID 를 넣는다.
 * 고른 뒤 이름을 다시 고치면 고른 값을 지운다 (다른 사람에게 잘못 보내지 않게).
 */
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('[data-compose]');
    if (!form) return;
    const search = form.querySelector('[data-receiver-search]');
    const results = form.querySelector('[data-receiver-results]');
    const hiddenId = form.querySelector('[data-receiver-id]');
    const chosen = form.querySelector('[data-receiver-chosen]');
    let timer;

    const choose = (receiver) => {
        hiddenId.value = receiver.memberId;
        search.value = receiver.nickname;
        chosen.textContent = `받는 사람: ${receiver.nickname}`;
        results.hidden = true;
    };

    const render = (receivers) => {
        results.replaceChildren(...receivers.map((receiver) => {
            const li = document.createElement('li');
            const button = document.createElement('button');
            button.type = 'button';
            button.textContent = receiver.nickname;
            button.addEventListener('click', () => choose(receiver));
            li.append(button);
            return li;
        }));
        results.hidden = receivers.length === 0;
    };

    search.addEventListener('input', () => {
        hiddenId.value = '';
        chosen.textContent = '';
        clearTimeout(timer);
        const nickname = search.value.trim();
        if (!nickname) {
            render([]);
            return;
        }
        timer = setTimeout(async () => {
            try {
                const response = await fetch(`/api/users?nickname=${encodeURIComponent(nickname)}`, { headers: { Accept: 'application/json' } });
                const body = await response.json().catch(() => null);
                render(response.ok && Array.isArray(body) ? body : []);
            } catch (e) {
                render([]);
            }
        }, 250);
    });
});

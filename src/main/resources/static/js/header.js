/*
 * 공통 헤더(common/layout.html)의 햄버거 버튼: 모바일에서 메뉴를 펼치고 접는다 (설계서 7장, #153).
 * 화면 안에서만 돈다. 데스크톱에서는 버튼이 숨겨져 있어 아무 일도 하지 않는다.
 * 아래 두 번째 묶음은 알림 드롭다운 (1-5, #185): 열 때 API-NTF-01 1쪽, "더 보기"로 다음 쪽, "모두 읽음"은 API-NTF-03.
 * 항목은 /notifications/{id} 링크 하나다 (서버가 읽음을 남기고 도착지로 보낸다).
 */
(() => {
    const header = document.querySelector('[data-header]');
    const toggle = header && header.querySelector('[data-header-toggle]');
    if (!toggle) return;

    const setOpen = (open) => {
        header.classList.toggle('is-open', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? '메뉴 닫기' : '메뉴 열기');
    };

    toggle.addEventListener('click', () => setOpen(!header.classList.contains('is-open')));
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && header.classList.contains('is-open')) {
            setOpen(false);
            toggle.focus();
        }
    });
})();

(() => {
    const root = document.querySelector('[data-notify]');
    if (!root) return;
    const toggle = root.querySelector('[data-notify-toggle]');
    const panel = root.querySelector('[data-notify-panel]');
    const list = root.querySelector('[data-notify-list]');
    const empty = root.querySelector('[data-notify-empty]');
    const more = root.querySelector('[data-notify-more]');
    const readAll = root.querySelector('[data-notify-read-all]');
    let nextPage = 1;
    let loading = false;

    const showError = (message) => alert(message ?? '알림을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.');

    const setCount = (count) => {
        let badge = root.querySelector('[data-notify-count]');
        toggle.setAttribute('aria-label', count > 0 ? `알림, 안 읽은 알림 ${count}개` : '알림');
        if (count <= 0) {
            badge?.remove();
            return;
        }
        if (!badge) {
            badge = document.createElement('span');
            badge.className = 'vl-notify__count';
            badge.setAttribute('aria-hidden', 'true');
            badge.dataset.notifyCount = '';
            toggle.append(badge);
        }
        badge.textContent = count > 99 ? '99+' : String(count);
    };

    // "2026-11-09T10:05:00" → "11.09 10:05"
    const when = (createdAt) => `${createdAt.slice(5, 7)}.${createdAt.slice(8, 10)} ${createdAt.slice(11, 16)}`;

    const render = (items) => {
        items.forEach((item) => {
            const li = document.createElement('li');
            const link = document.createElement('a');
            link.className = item.read ? 'vl-notify__item' : 'vl-notify__item is-unread';
            link.href = `/notifications/${item.notificationId}`;
            const text = document.createElement('span');
            text.textContent = item.content;
            const time = document.createElement('time');
            time.dateTime = item.createdAt;
            time.textContent = when(item.createdAt);
            link.append(text, time);
            li.append(link);
            list.append(li);
        });
    };

    const load = async () => {
        if (loading) return;
        loading = true;
        try {
            const response = await fetch(`/api/notifications?page=${nextPage}`, { headers: { Accept: 'application/json' } });
            const body = await response.json().catch(() => null);
            if (!response.ok || !body) {
                showError(body?.message);
                return;
            }
            render(body.items);
            setCount(body.unreadCount);
            empty.hidden = list.children.length > 0;
            more.hidden = !body.hasNext;
            nextPage += 1;
        } catch (e) {
            showError();
        } finally {
            loading = false;
        }
    };

    const setOpen = (open) => {
        panel.hidden = !open;
        toggle.setAttribute('aria-expanded', String(open));
        if (open && nextPage === 1) load();
    };

    toggle.addEventListener('click', () => setOpen(panel.hidden));
    more.addEventListener('click', load);
    readAll.addEventListener('click', async () => {
        readAll.disabled = true;
        try {
            const response = await fetch('/api/notifications/read-all', {
                method: 'POST',
                headers: { [root.dataset.csrfHeader]: root.dataset.csrfToken },
            });
            if (response.status !== 204) {
                const body = await response.json().catch(() => null);
                showError(body?.message ?? '모두 읽음으로 바꾸지 못했습니다. 잠시 후 다시 시도해 주세요.');
                return;
            }
            list.querySelectorAll('.is-unread').forEach((item) => item.classList.remove('is-unread'));
            setCount(0);
        } catch (e) {
            showError();
        } finally {
            readAll.disabled = false;
        }
    });
    document.addEventListener('click', (event) => {
        if (!panel.hidden && !root.contains(event.target)) setOpen(false);
    });
    document.addEventListener('keydown', (event) => {
        if (event.key === 'Escape' && !panel.hidden) {
            setOpen(false);
            toggle.focus();
        }
    });
})();

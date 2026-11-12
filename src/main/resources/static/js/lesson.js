/*
 * 수업 화면(U-13) 안에서만 도는 동작: 활동 목록 선택 → 오른쪽 내용 전환, activity 쿼리 갱신.
 * 서버는 모든 활동 패널(잠김·완료·진행 중)을 한 번에 내려주고(API-LSN-01, #107), 여기서는 보이기/숨기기만 한다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const nav = document.querySelector('[data-activity-nav]');
    const content = document.querySelector('[data-activity-content]');
    if (!nav || !content) return;

    const buttons = [...nav.querySelectorAll('[data-activity-btn]')];
    const panels = [...content.querySelectorAll('[data-activity-panel]')];

    const panelOf = (id) => panels.find((panel) => panel.dataset.activityPanel === id);

    // 기본값: URL 의 activity 쿼리를 쓰고, 없거나 모르는 값이면 템플릿에서 처음부터 보이는 활동을 쓴다.
    const requested = new URLSearchParams(window.location.search).get('activity');
    const initial = panelOf(requested)
        ? requested
        : (panels.find((panel) => !panel.hidden)?.dataset.activityPanel ?? buttons[0]?.dataset.activity);

    const select = (id, { focus = false } = {}) => {
        const panel = panelOf(id);
        if (!panel) return;

        panels.forEach((p) => { p.hidden = p !== panel; });
        buttons.forEach((button) => button.setAttribute('aria-current', String(button.dataset.activity === id)));

        const url = new URL(window.location.href);
        url.searchParams.set('activity', id);
        window.history.replaceState(null, '', url);

        if (focus) {
            panel.setAttribute('tabindex', '-1');
            panel.focus();
        }
    };

    buttons.forEach((button) => {
        button.addEventListener('click', () => select(button.dataset.activity));
    });

    content.querySelectorAll('[data-activity-link]').forEach((link) => {
        link.addEventListener('click', () => select(link.dataset.activityLink, { focus: true }));
    });

    if (initial) select(initial);
});

/*
 * 개념 북마크 토글(API-LSN-08)과 개념 태그 검색 모달(API-LSN-09).
 * 서버가 준 값(제목·이름)은 모두 textContent 로만 넣는다 (XSS). 주소에 들어가는 id 는 숫자로 바꿔서 쓴다.
 * 모달 열기·닫기는 공통 ui.js(data-ui-modal-open)가 하고, 여기서는 검색 결과만 채운다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const csrfHeaders = () => {
        const token = document.querySelector('meta[name="_csrf"]')?.content;
        const header = document.querySelector('meta[name="_csrf_header"]')?.content;
        return token && header ? { [header]: token } : {};
    };

    // 북마크: 누를 때마다 켜고 끈다. 서버가 돌려준 값으로 표시를 바꾼다
    document.querySelectorAll('[data-bookmark]').forEach((button) => {
        button.addEventListener('click', async () => {
            button.disabled = true;
            try {
                const response = await fetch(button.dataset.url, { method: 'POST', headers: csrfHeaders() });
                const body = await response.json().catch(() => null);
                if (response.ok && body) {
                    button.setAttribute('aria-pressed', String(body.bookmarked));
                    button.textContent = body.bookmarked ? '북마크 해제' : '북마크';
                } else {
                    alert(body?.message ?? '북마크를 처리하지 못했습니다. 화면을 새로고침한 뒤 다시 시도해 주세요.');
                }
            } catch (e) {
                alert('요청을 보내지 못했습니다. 잠시 후 다시 시도해 주세요.');
            } finally {
                button.disabled = false;
            }
        });
    });

    // 개념 태그 칩 → 같은 태그의 개념 찾기
    const modal = document.querySelector('[data-ui-modal="concept-search-modal"]');
    if (!modal) return;
    const title = modal.querySelector('[data-concept-search-title]');
    const message = modal.querySelector('[data-concept-search-message]');
    const list = modal.querySelector('[data-concept-search-list]');
    let requestNo = 0;

    const renderResults = (items) => {
        list.replaceChildren();
        if (items.length === 0) {
            message.textContent = '이 태그가 달린 개념이 없습니다.';
            return;
        }
        message.textContent = '';
        items.forEach((item) => {
            const li = document.createElement('li');
            const link = document.createElement('a');
            link.className = 'ls-search-link';
            link.href = `/courses/${Number(item.courseId)}/lessons/${Number(item.lessonId)}?activity=${Number(item.activityId)}`;
            const name = document.createElement('span');
            name.className = 'ls-search-name';
            name.textContent = item.title;
            const where = document.createElement('span');
            where.className = 'ls-search-where';
            where.textContent = `${item.courseTitle} · ${item.lessonTitle}`;
            link.append(name, where);
            li.appendChild(link);
            list.appendChild(li);
        });
    };

    document.querySelectorAll('[data-concept-tag]').forEach((chip) => {
        chip.addEventListener('click', async () => {
            const current = ++requestNo;
            title.textContent = `#${chip.dataset.conceptTagName} 개념`;
            list.replaceChildren();
            message.textContent = '불러오는 중입니다.';
            try {
                const url = `${modal.dataset.searchUrl}?conceptTagId=${encodeURIComponent(chip.dataset.conceptTagId)}`;
                const response = await fetch(url, { headers: { Accept: 'application/json' } });
                const body = await response.json().catch(() => null);
                if (current !== requestNo) return; // 그 사이 다른 태그를 눌렀으면 이 결과는 버린다
                if (response.ok && Array.isArray(body)) {
                    renderResults(body);
                } else {
                    message.textContent = '검색 결과를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
                }
            } catch (e) {
                if (current === requestNo) {
                    message.textContent = '검색 결과를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.';
                }
            }
        });
    });
});

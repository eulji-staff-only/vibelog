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

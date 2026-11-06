/*
 * 내 학습 화면(U-12) 안에서만 도는 동작: 탭(수강 중 · 완료) 전환, tab 쿼리 갱신.
 * 서버를 부르지 않는다. 코스·수업·포트폴리오 상태는 ③ 단계에서 API 명세대로 붙인다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const nav = document.querySelector('[data-tab-nav]');
    if (!nav) return;

    const buttons = [...nav.querySelectorAll('[data-tab-btn]')];
    const panels = [...document.querySelectorAll('[data-tab-panel]')];

    const panelOf = (id) => panels.find((panel) => panel.dataset.tabPanel === id);

    // 기본값: URL 의 tab 쿼리를 쓰고, 없거나 모르는 값이면 첫 번째 탭(수강 중)을 쓴다.
    const requested = new URLSearchParams(window.location.search).get('tab');
    const initial = panelOf(requested) ? requested : buttons[0]?.dataset.tab;

    const select = (id) => {
        const panel = panelOf(id);
        if (!panel) return;

        panels.forEach((p) => { p.hidden = p !== panel; });
        buttons.forEach((button) => button.setAttribute('aria-selected', String(button.dataset.tab === id)));

        const url = new URL(window.location.href);
        url.searchParams.set('tab', id);
        window.history.replaceState(null, '', url);
    };

    buttons.forEach((button) => {
        button.addEventListener('click', () => select(button.dataset.tab));
    });

    if (initial) select(initial);
});

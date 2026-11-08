/*
 * 공통 헤더(common/layout.html)의 햄버거 버튼: 모바일에서 메뉴를 펼치고 접는다 (설계서 7장, #153).
 * 화면 안에서만 돈다. 데스크톱에서는 버튼이 숨겨져 있어 아무 일도 하지 않는다.
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

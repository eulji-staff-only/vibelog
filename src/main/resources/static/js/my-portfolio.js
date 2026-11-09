/*
 * 내 포트폴리오 화면(U-19) 안에서만 도는 동작: 공개·비공개 표시 바꾸기, 삭제 확인 모달(M-04) 열고 닫기.
 * 서버를 부르지 않는다. 공개 전환(API-PRT-08)과 삭제(API-PRT-07)는 ③ 단계에서 붙인다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const list = document.querySelector('[data-mp-list]');
    const modal = document.querySelector('[data-delete-modal]');
    if (!list || !modal) return;

    // 공개 전환: 배지와 버튼 문구만 바꾼다
    list.querySelectorAll('[data-vis-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            const badge = button.closest('.mp-card').querySelector('[data-vis-badge]');
            const toPublic = badge.textContent.trim() === '비공개';
            badge.textContent = toPublic ? '공개' : '비공개';
            badge.classList.toggle('mp-badge-strong', toPublic);
            badge.classList.toggle('mp-badge-neutral', !toPublic);
            button.textContent = toPublic ? '비공개로 전환' : '공개로 전환';
        });
    });

    // M-04 삭제 확인
    let opener = null;
    const open = (button) => {
        opener = button;
        modal.hidden = false;
        modal.querySelector('#mp-delete-title').focus();
    };
    const close = () => {
        modal.hidden = true;
        if (opener) opener.focus();
    };

    list.querySelectorAll('[data-open-delete]').forEach((button) => button.addEventListener('click', () => open(button)));
    modal.querySelectorAll('[data-close-delete]').forEach((button) => button.addEventListener('click', close));
    modal.addEventListener('click', (e) => { if (e.target === modal) close(); });
    modal.addEventListener('keydown', (e) => { if (e.key === 'Escape') close(); });
});

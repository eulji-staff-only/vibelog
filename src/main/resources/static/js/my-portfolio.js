/*
 * 내 포트폴리오 화면(U-19): 공개 전환(API-PRT-08, JSON)과 삭제 확인 모달(M-04).
 * 공개 전환은 새로고침 없이 배지·버튼을 바꾸고 토스트를 띄운다. 삭제는 모달의 폼으로 POST 한다(API-PRT-07).
 */
document.addEventListener('DOMContentLoaded', () => {
    const list = document.querySelector('[data-mp-list]');
    const modal = document.querySelector('[data-delete-modal]');
    if (!list || !modal) return;

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    const toast = (message, variant) => window.VLToast && window.VLToast.show(message, { variant });

    // 공개 전환: 서버가 돌려준 값으로 배지·버튼 문구를 바꾼다. 누르는 동안 버튼을 잠가 두 번 보내지 않는다
    list.querySelectorAll('[data-vis-toggle]').forEach((button) => {
        button.addEventListener('click', () => {
            if (button.disabled) return;
            button.disabled = true;
            fetch(button.dataset.visibilityUrl, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
                body: JSON.stringify({ isPublic: button.dataset.public !== 'true' }),
            })
                .then((res) => res.json().then((body) => {
                    if (!res.ok) throw body;
                    return body;
                }))
                .then(({ isPublic }) => {
                    const badge = button.closest('.mp-card').querySelector('[data-vis-badge]');
                    badge.textContent = isPublic ? '공개' : '비공개';
                    badge.classList.toggle('mp-badge-strong', isPublic);
                    badge.classList.toggle('mp-badge-neutral', !isPublic);
                    button.dataset.public = String(isPublic);
                    button.textContent = isPublic ? '비공개로 전환' : '공개로 전환';
                    toast(isPublic ? '공개로 전환했습니다' : '비공개로 전환했습니다');
                })
                .catch((err) => {
                    // 네트워크 끊김 같은 브라우저 오류(Error)는 기술 문구라 보여 주지 않는다 (PL-ERR-001)
                    toast(err instanceof Error || !err.message
                        ? '바꾸지 못했습니다. 잠시 후 다시 시도해 주세요.' : err.message, 'danger');
                })
                .finally(() => { button.disabled = false; });
        });
    });

    // M-04 삭제 확인: 누른 카드의 삭제 주소를 폼에 넣는다
    const form = modal.querySelector('[data-delete-form]');
    let opener = null;
    const open = (button) => {
        opener = button;
        form.action = button.dataset.deleteUrl;
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
    // 두 번 눌러 두 번 보내지 않게 (PL-ERR-003)
    form.addEventListener('submit', () => { form.querySelector('[type="submit"]').disabled = true; });
});

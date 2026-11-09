/*
 * A-07 포트폴리오 과제 편집(API-ADM-35) 화면 안에서만 도는 동작: 스텝 추가·삭제·위아래로 옮기기.
 * 서버를 부르지 않는다. 바꿀 때마다 name 을 steps[0].guideStepId, steps[0].content … 처럼
 * 화면 순서대로 다시 붙여서, 저장하면 보낸 순서가 곧 스텝 순서가 된다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const list = document.querySelector('[data-steps]');
    if (!list) return;
    const template = document.querySelector('[data-step-template]');

    const renumber = () => {
        [...list.querySelectorAll('[data-step]')].forEach((step, i) => {
            const id = step.querySelector('[data-step-id]');
            const content = step.querySelector('[data-step-content]');
            id.name = `steps[${i}].guideStepId`;
            id.removeAttribute('id');
            content.name = `steps[${i}].content`;
            content.removeAttribute('id');
        });
    };

    list.addEventListener('click', (e) => {
        const step = e.target.closest('[data-step]');
        if (!step) return;
        if (e.target.closest('[data-step-remove]')) {
            step.remove();
        } else if (e.target.closest('[data-step-up]') && step.previousElementSibling) {
            list.insertBefore(step, step.previousElementSibling);
        } else if (e.target.closest('[data-step-down]') && step.nextElementSibling) {
            list.insertBefore(step.nextElementSibling, step);
        } else {
            return;
        }
        renumber();
    });

    document.querySelector('[data-step-add]').addEventListener('click', () => {
        const step = template.content.firstElementChild.cloneNode(true);
        list.appendChild(step);
        renumber();
        step.querySelector('[data-step-content]').focus();
    });

    renumber();
});

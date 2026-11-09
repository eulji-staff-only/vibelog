/*
 * A-07 포트폴리오 과제 편집 화면 안에서만 도는 동작: 스텝(API-ADM-35)·FAQ(API-PRT-11) 목록의 추가·삭제·위아래로 옮기기.
 * A-12 템플릿 관리(API-PRT-15)의 항목 목록도 같은 방식으로 위아래로만 옮긴다 (항목 고정이라 추가·삭제 버튼이 없다).
 * 서버를 부르지 않는다. 바꿀 때마다 name 을 steps[0].guideStepId, faqs[0].question … 처럼
 * 화면 순서대로 다시 붙여서, 저장하면 보낸 순서가 곧 순서(sort_order)가 된다.
 *
 * 목록: data-list="steps|faqs", 한 줄: data-item, 칸: data-field="필드 이름",
 * 버튼: data-item-up · data-item-down · data-item-remove, 추가: data-add="목록 이름", 빈 줄: <template data-template="목록 이름">
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-list]').forEach((list) => {
        const name = list.dataset.list;
        const template = document.querySelector(`[data-template="${name}"]`);

        const renumber = () => {
            [...list.querySelectorAll('[data-item]')].forEach((item, i) => {
                item.querySelectorAll('[data-field]').forEach((field) => {
                    field.name = `${name}[${i}].${field.dataset.field}`;
                    field.removeAttribute('id');
                });
            });
        };

        list.addEventListener('click', (e) => {
            const item = e.target.closest('[data-item]');
            if (!item) return;
            if (e.target.closest('[data-item-remove]')) {
                item.remove();
            } else if (e.target.closest('[data-item-up]') && item.previousElementSibling) {
                list.insertBefore(item, item.previousElementSibling);
            } else if (e.target.closest('[data-item-down]') && item.nextElementSibling) {
                list.insertBefore(item.nextElementSibling, item);
            } else {
                return;
            }
            renumber();
        });

        const add = document.querySelector(`[data-add="${name}"]`);
        if (add) add.addEventListener('click', () => {
            const item = template.content.firstElementChild.cloneNode(true);
            list.appendChild(item);
            renumber();
            item.querySelector('[data-field]:not([type="hidden"])').focus();
        });

        renumber();
    });
});

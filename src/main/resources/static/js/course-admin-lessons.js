/*
 * [course-admin-lessons.js]
 * 역할 : 운영자 코스 상세(A-05, #96)의 수업 순서 바꾸기.
 * 책임 : - 끌거나(↑·↓ 버튼으로도) 순서를 바꾸면 "순서 저장" 버튼을 켠다 (화면 흐름 4장 "수업 드래그 → 저장 버튼 활성").
 *        - 저장은 숨은 lessonIds 칸이 바뀐 순서대로 폼에 실려 API-ADM-18 로 간다 (서버를 부르는 JS 는 없다).
 */
(() => {
    const form = document.querySelector('[data-ca-lesson-order]');
    if (!form) return;

    const list = form.querySelector('.ca-order');
    const saveButton = form.querySelector('[data-ca-order-save]');
    const changed = () => { saveButton.disabled = false; };

    let dragging = null;
    list.querySelectorAll('li[draggable="true"]').forEach((row) => {
        row.addEventListener('dragstart', (e) => {
            dragging = row;
            // Firefox 는 dataTransfer 에 값이 없으면 드래그를 시작하지 않는다
            e.dataTransfer.effectAllowed = 'move';
            e.dataTransfer.setData('text/plain', '');
            row.classList.add('is-dragging');
        });
        row.addEventListener('dragend', () => {
            row.classList.remove('is-dragging');
            dragging = null;
        });
        row.addEventListener('dragover', (e) => {
            e.preventDefault();
            if (!dragging || dragging === row) return;
            const rect = row.getBoundingClientRect();
            const putAfter = e.clientY > rect.top + rect.height / 2;
            const before = putAfter ? row.nextSibling : row;
            if (before === dragging || before === dragging.nextSibling) return;
            list.insertBefore(dragging, before);
            changed();
        });
    });

    // 키보드로도 순서를 바꿀 수 있게 ↑·↓ 버튼 (드래그는 마우스만 된다)
    list.querySelectorAll('[data-ca-move]').forEach((button) => {
        button.addEventListener('click', () => {
            const row = button.closest('li');
            const target = button.dataset.caMove === 'up' ? row.previousElementSibling : row.nextElementSibling;
            if (!target) return;
            list.insertBefore(row, button.dataset.caMove === 'up' ? target : target.nextSibling);
            button.focus();
            changed();
        });
    });
})();

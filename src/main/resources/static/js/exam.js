/*
 * 모의고사 화면(U-15) 안에서만 도는 동작: 문항 전환, 보기 선택, 미선택 수 계산, 제출 확인 모달(M-02).
 * 서버를 부르지 않는다. 답 저장(API-EXM-03)과 제출(API-EXM-04)은 ③ 단계에서 붙인다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('[data-exam]');
    if (!root) return;

    const questions = [...root.querySelectorAll('[data-question]')];
    const navButtons = [...root.querySelectorAll('[data-go]')];
    const total = questions.length;
    const answers = new Array(total).fill(null);
    let current = 0;

    const modal = document.querySelector('[data-submit-modal]');
    const openButton = root.querySelector('[data-open-submit]');

    const show = (index) => {
        current = Math.max(0, Math.min(total - 1, index));
        render();
    };

    const render = () => {
        questions.forEach((q, i) => { q.hidden = i !== current; });

        const answered = answers.filter((a) => a !== null).length;
        const unanswered = total - answered;

        navButtons.forEach((button, i) => {
            const done = answers[i] !== null;
            button.classList.toggle('is-answered', done);
            if (i === current) button.setAttribute('aria-current', 'step');
            else button.removeAttribute('aria-current');
            button.setAttribute('aria-label', `문항 ${i + 1}, ${done ? '응답함' : '미선택'}${i === current ? ', 현재 문항' : ''}`);
        });

        root.querySelector('[data-prev]').disabled = current === 0;
        root.querySelector('[data-next]').disabled = current === total - 1;

        root.querySelector('[data-answered]').textContent = answered;
        root.querySelector('[data-progress]').setAttribute('aria-valuenow', answered);
        root.querySelector('[data-progress-fill]').style.width = `${(answered / total) * 100}%`;

        root.querySelector('[data-unanswered-count]').textContent = unanswered;
        root.querySelector('[data-unanswered-area]').hidden = unanswered === 0;
        root.querySelector('[data-all-answered]').hidden = unanswered !== 0;

        const list = root.querySelector('[data-unanswered-list]');
        list.replaceChildren(...answers.flatMap((a, i) => {
            if (a !== null) return [];
            const button = document.createElement('button');
            button.type = 'button';
            button.className = 'exam-btn exam-btn-outline';
            button.textContent = `문항 ${i + 1}`;
            button.addEventListener('click', () => show(i));
            return [button];
        }));

        const modalCount = modal.querySelector('[data-modal-count]');
        modalCount.textContent = `${unanswered}개`;
        modalCount.classList.toggle('is-zero', unanswered === 0);
    };

    questions.forEach((q, qi) => {
        const choices = [...q.querySelectorAll('.exam-choice')];
        choices.forEach((choice, ci) => {
            choice.addEventListener('click', () => {
                answers[qi] = ci;
                choices.forEach((c, i) => c.setAttribute('aria-pressed', String(i === ci)));
                render();
            });
        });
    });

    navButtons.forEach((button) => button.addEventListener('click', () => show(Number(button.dataset.go))));
    root.querySelector('[data-prev]').addEventListener('click', () => show(current - 1));
    root.querySelector('[data-next]').addEventListener('click', () => show(current + 1));

    const openModal = () => {
        modal.hidden = false;
        modal.querySelector('#exam-submit-title').focus();
    };
    const closeModal = () => {
        modal.hidden = true;
        openButton.focus();
    };

    openButton.addEventListener('click', openModal);
    modal.querySelector('[data-close-submit]').addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    modal.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

    render();
});

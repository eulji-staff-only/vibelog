/*
 * 모의고사 화면(U-15): 문항 전환, 보기 선택, 미선택 수 계산, 제출 확인 모달(M-02).
 * 보기를 고를 때마다 답을 저장한다 (API-EXM-03, PL-EXM-006). 고른 보기를 다시 누르면 선택 취소.
 * 제출(API-EXM-04)할 때는 모든 문항의 마지막 답을 answers[문항 ID]=보기 ID 숨은 칸으로 함께 보낸다.
 * 고르지 않은 문항은 빈 값으로 보내, 취소 저장이 실패했어도 서버에 남은 이전 답이 채점되지 않게 한다.
 * 저장이 실패해도 제출 폼이 마지막 답을 다시 보내므로 화면의 선택은 그대로 둔다.
 */
document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('[data-exam]');
    if (!root) return;

    const questions = [...root.querySelectorAll('[data-question]')];
    const navButtons = [...root.querySelectorAll('[data-go]')];
    const total = questions.length;
    // 문항별로 고른 보기 버튼 순서(없으면 null). 서버가 그린 aria-pressed 로 이어 풀기 상태를 읽는다
    const answers = questions.map((q) => {
        const index = [...q.querySelectorAll('.exam-choice')].findIndex((c) => c.getAttribute('aria-pressed') === 'true');
        return index < 0 ? null : index;
    });
    const answerUrl = root.dataset.answerUrl;
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;

    const saveAnswer = (examQuestionId, examChoiceId) => {
        fetch(answerUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
            body: JSON.stringify({ examQuestionId, examChoiceId }),
        }).catch(() => {});
    };
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
        root.querySelector('[data-next]').disabled = current >= total - 1;

        root.querySelector('[data-answered]').textContent = answered;
        root.querySelector('[data-progress]').setAttribute('aria-valuenow', answered);
        root.querySelector('[data-progress-fill]').style.width = `${total ? (answered / total) * 100 : 0}%`;

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
                answers[qi] = answers[qi] === ci ? null : ci;
                choices.forEach((c, i) => c.setAttribute('aria-pressed', String(i === answers[qi])));
                saveAnswer(Number(q.dataset.questionId),
                    answers[qi] === null ? null : Number(choice.dataset.choiceId));
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
    modal.querySelector('[data-submit-form]').addEventListener('submit', (e) => {
        const form = e.currentTarget;
        form.querySelector('button[type="submit"]').disabled = true;   // 중복 제출 막기 (PL-ERR-003)
        questions.forEach((q, qi) => {
            const input = document.createElement('input');
            input.type = 'hidden';
            input.name = `answers[${q.dataset.questionId}]`;
            // 미선택은 빈 값으로 보내 서버에 남은 이전 답을 취소한다
            input.value = answers[qi] === null ? '' : q.querySelectorAll('.exam-choice')[answers[qi]].dataset.choiceId;
            form.appendChild(input);
        });
    });
    modal.querySelector('[data-close-submit]').addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => { if (e.target === modal) closeModal(); });
    modal.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeModal(); });

    render();
});

/*
 * quiz.js — 퀴즈 화면(U-14, #54·#108).
 *
 * - 화면을 열면 풀이 시작(API-LSN-04)을 불러 문항을 받아 그린다. 응답에는 정답이 없다.
 * - 보기 선택 → "확인" → 문항 확인(API-LSN-05)이 정답·해설을 준다. 확인한 문항은 답을 바꿀 수 없다 (PL-LSN-006).
 *   낚시 문제(TRICK)는 물고기를 누르면 확인 버튼 없이 바로 확인한다 (설계서 U-14).
 * - 마지막 문항 뒤 "결과 보기" → 제출·채점(API-LSN-06) 결과를 같은 화면에 그린다. 통과하면 열린 것을 토스트로 알린다.
 * - "퀴즈 다시 풀기"는 풀이 시작을 다시 부른다 (서버가 문항·보기 순서를 섞어 준다, PL-LSN-004·005).
 * - 서버 값은 모두 textContent 로 넣는다 (innerHTML 을 쓰지 않는다).
 * - 실패 응답은 {code, message} JSON 이다. message 를 오류 토스트로 보여 준다.
 */
(function () {
    'use strict';

    const root = document.querySelector('[data-quiz]');
    const solve = root && root.querySelector('[data-quiz-solve]');
    if (!root || !solve) return;   // 문항이 없는 퀴즈는 안내 상자만 보인다

    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    const result = root.querySelector('[data-quiz-result]');
    const list = root.querySelector('[data-quiz-questions]');
    const loading = root.querySelector('[data-quiz-loading]');
    const checkBtn = root.querySelector('[data-quiz-check]');
    const nextBtn = root.querySelector('[data-quiz-next]');
    const reason = root.querySelector('[data-quiz-reason]');
    const counter = root.querySelector('[data-quiz-counter]');
    const progress = root.querySelector('[data-quiz-progress]');
    const progressFill = root.querySelector('[data-quiz-progress-fill]');

    const TYPE_LABEL = { CODE_BLANK: '코드 빈칸 채우기', CONCEPT: '개념 문제', OUTPUT_PREDICT: '결과 유추하기', TRICK: '낚시 문제' };
    const TYPE_CLASS = { CODE_BLANK: 'is-blank', CONCEPT: 'is-concept', OUTPUT_PREDICT: 'is-output', TRICK: 'is-trick' };
    const NEXT_LABEL = { CONCEPT: '다음 개념으로', EXAM: '모의고사로', NONE: '수업으로' };

    let attemptId = null;
    let questions = [];      // 서버가 준 문항
    let sections = [];       // 문항마다 그린 section
    let index = 0;
    let picked = {};         // 문항 번호 → 고른 보기 ID
    let checked = {};        // 문항 번호 → 확인했는지
    let busy = false;

    /* ---- 서버 호출 ---- */
    async function post(url, body) {
        const headers = { [csrfHeader]: csrfToken, Accept: 'application/json' };
        if (body) headers['Content-Type'] = 'application/json';
        const response = await fetch(url, { method: 'POST', headers, body: body ? JSON.stringify(body) : undefined });
        const data = await response.json().catch(() => ({}));
        if (!response.ok) throw new Error(data.message || '잠시 후 다시 시도해 주세요.');
        return data;
    }

    function showError(error) {
        if (window.VLToast) window.VLToast.show(error.message, { variant: 'danger' });
    }

    /* ---- 문항 그리기 ---- */
    function el(tag, className, text) {
        const node = document.createElement(tag);
        if (className) node.className = className;
        if (text != null) node.textContent = text;
        return node;
    }

    function codeBlock(code, withBlank) {
        const pre = el('pre', 'qz-code');
        const codeEl = el('code');
        // 코드 빈칸 채우기: 밑줄 3개 이상(___)을 빈칸으로 그린다. 없으면 코드만 보인다
        const parts = withBlank ? code.split(/_{3,}/) : [code];
        parts.forEach((part, i) => {
            codeEl.appendChild(document.createTextNode(part));
            if (i < parts.length - 1) {
                const blank = el('span', 'qz-blank', '　');
                blank.setAttribute('data-quiz-blank', '');
                blank.setAttribute('aria-label', '빈칸');
                codeEl.appendChild(blank);
            }
        });
        pre.appendChild(codeEl);
        return pre;
    }

    function renderQuestion(question, qi) {
        const section = el('section', 'qz-question');
        section.setAttribute('aria-label', '문항 ' + (qi + 1));
        section.hidden = qi !== 0;

        section.appendChild(el('span', 'qz-type ' + (TYPE_CLASS[question.displayType] || ''), TYPE_LABEL[question.displayType] || ''));
        const title = el('h2', 'qz-q', question.content);
        title.tabIndex = -1;
        section.appendChild(title);

        const type = question.displayType;
        if ((type === 'CODE_BLANK' || type === 'OUTPUT_PREDICT') && question.codeSnippet) {
            section.appendChild(codeBlock(question.codeSnippet, type === 'CODE_BLANK'));
        }

        let group;
        if (type === 'TRICK') {
            group = el('div', 'qz-pond');
            group.setAttribute('aria-label', '보기: 물고기');
        } else {
            group = el('div', 'qz-choices ' + (type === 'CODE_BLANK' ? 'is-chips' : type === 'OUTPUT_PREDICT' ? 'is-grid' : 'is-list'));
            group.setAttribute('aria-label', '보기');
        }
        group.setAttribute('role', 'group');

        question.choices.forEach((choice, ci) => {
            const button = el('button', 'qz-choice');
            button.type = 'button';
            button.setAttribute('aria-pressed', 'false');
            button.dataset.choiceId = String(choice.quizChoiceId);
            if (type === 'TRICK') {
                button.classList.add('qz-fish', 'is-p' + (ci + 1));
                button.insertAdjacentHTML('beforeend', '<svg viewBox="0 0 150 72" aria-hidden="true"><path class="qz-fish__fin" d="M30 38 L4 14 Q14 38 4 62 Z"/><ellipse class="qz-fish__body" cx="82" cy="38" rx="56" ry="27"/><circle class="qz-fish__eye" cx="122" cy="32" r="4"/></svg>');
                button.appendChild(el('span', null, choice.content));
            } else {
                if (type !== 'CONCEPT') button.classList.add('is-mono');
                button.textContent = choice.content;
            }
            button.addEventListener('click', () => pick(qi, choice.quizChoiceId));
            group.appendChild(button);
        });
        section.appendChild(group);

        if (type === 'TRICK') section.appendChild(el('p', 'qz-hint', '물고기를 눌러 낚아 보세요.'));

        const feedback = el('div', 'qz-feedback');
        feedback.setAttribute('role', 'status');
        feedback.hidden = true;
        feedback.appendChild(el('strong'));
        feedback.appendChild(el('p'));
        section.appendChild(feedback);
        return section;
    }

    /* ---- 진행 상태 ---- */
    function render(moveFocus) {
        sections.forEach((s, i) => { s.hidden = i !== index; });
        const total = questions.length;
        const done = Object.keys(checked).length;
        const isChecked = !!checked[index];
        const isTrick = questions[index].displayType === 'TRICK';

        counter.textContent = (index + 1) + ' / ' + total;
        progress.setAttribute('aria-valuenow', String(done));
        progressFill.style.width = Math.round(done / total * 100) + '%';

        checkBtn.hidden = isChecked || isTrick;
        reason.hidden = isChecked || isTrick || picked[index] != null;
        checkBtn.setAttribute('aria-disabled', picked[index] == null || busy ? 'true' : 'false');
        nextBtn.hidden = !isChecked;
        nextBtn.textContent = index === total - 1 ? '결과 보기' : '다음';
        if (moveFocus) sections[index].querySelector('.qz-q').focus();
    }

    function choicesOf(qi) {
        return Array.from(sections[qi].querySelectorAll('.qz-choice'));
    }

    function pick(qi, choiceId) {
        if (checked[qi] || busy) return;
        picked[qi] = choiceId;
        choicesOf(qi).forEach((c) => c.setAttribute('aria-pressed', String(c.dataset.choiceId === String(choiceId))));
        const blank = sections[qi].querySelector('[data-quiz-blank]');
        if (blank) {
            blank.textContent = choicesOf(qi).find((c) => c.dataset.choiceId === String(choiceId)).textContent;
            blank.classList.add('is-filled');
        }
        if (questions[qi].displayType === 'TRICK') {
            check();
            return;
        }
        render(false);
    }

    /* ---- API-LSN-05 문항 확인 ---- */
    async function check() {
        const qi = index;
        if (picked[qi] == null || checked[qi] || busy) return;
        busy = true;
        render(false);
        try {
            const data = await post('/api/quiz-attempts/' + attemptId + '/answers',
                { quizQuestionId: questions[qi].quizQuestionId, quizChoiceId: picked[qi] });
            checked[qi] = true;
            choicesOf(qi).forEach((c) => {
                const id = Number(c.dataset.choiceId);
                c.disabled = true;
                if (id === data.correctQuizChoiceId) c.classList.add('is-correct');
                else if (id === picked[qi]) c.classList.add('is-wrong');
                if (id === data.correctQuizChoiceId && data.correct && questions[qi].displayType === 'TRICK') c.classList.add('is-caught');
            });
            const feedback = sections[qi].querySelector('.qz-feedback');
            feedback.hidden = false;
            feedback.classList.toggle('is-correct', data.correct);
            feedback.classList.toggle('is-wrong', !data.correct);
            feedback.querySelector('strong').textContent = data.correct ? '정답입니다!' : '아쉬워요, 오답입니다.';
            feedback.querySelector('p').textContent = data.explanation || '';
        } catch (error) {
            showError(error);
        } finally {
            busy = false;
            render(false);
            if (checked[qi]) nextBtn.focus();
        }
    }

    /* ---- API-LSN-06 제출·채점 ---- */
    async function submit() {
        if (busy) return;
        busy = true;
        nextBtn.setAttribute('aria-disabled', 'true');
        try {
            const data = await post('/api/quiz-attempts/' + attemptId + '/submit');
            showResult(data);
        } catch (error) {
            showError(error);
        } finally {
            busy = false;
            nextBtn.removeAttribute('aria-disabled');
        }
    }

    function showResult(data) {
        const rate = Number(data.correctRate);
        root.querySelectorAll('[data-quiz-pass]').forEach((n) => { n.hidden = !data.isPassed; });
        root.querySelectorAll('[data-quiz-fail]').forEach((n) => { n.hidden = data.isPassed; });

        root.querySelector('[data-quiz-score]').setAttribute('aria-valuenow', String(Math.round(rate)));
        root.querySelector('[data-quiz-score-fill]').style.width = rate + '%';
        root.querySelector('[data-quiz-score-text]').textContent = rate.toFixed(0) + '%';

        if (data.isPassed) {
            root.querySelector('[data-quiz-pass-title]').textContent = data.message ? '통과했습니다. ' + data.message + '.' : '통과했습니다.';
            const link = root.querySelector('[data-quiz-next-link]');
            link.href = data.next.url;
            link.textContent = NEXT_LABEL[data.next.type] || '수업으로';
            if (data.message && window.VLToast) window.VLToast.show(data.message);
        }

        solve.hidden = true;
        result.hidden = false;
        result.querySelector('#qz-result-h').focus();
    }

    /* ---- API-LSN-04 풀이 시작 (처음·다시 풀기) ---- */
    async function start() {
        if (busy) return;   // "퀴즈 다시 풀기"를 빠르게 두 번 눌러도 풀이를 한 번만 시작한다
        busy = true;
        loading.hidden = false;
        list.replaceChildren();
        try {
            const data = await post(root.dataset.startUrl);
            attemptId = data.quizAttemptId;
            questions = data.questions;
            index = 0;
            picked = {};
            checked = {};
            sections = questions.map(renderQuestion);
            sections.forEach((s) => list.appendChild(s));
            loading.hidden = true;
            result.hidden = true;
            solve.hidden = false;
            busy = false;
            render(false);
        } catch (error) {
            loading.textContent = error.message;
            showError(error);
            busy = false;
        }
    }

    checkBtn.addEventListener('click', () => {
        if (checkBtn.getAttribute('aria-disabled') === 'true') return;
        check();
    });
    nextBtn.addEventListener('click', () => {
        if (index < questions.length - 1) {
            index += 1;
            render(true);
        } else {
            submit();
        }
    });
    root.querySelector('[data-quiz-retry]').addEventListener('click', () => {
        start().then(() => { if (sections[0]) sections[0].querySelector('.qz-q').focus(); });
    });

    start();
})();

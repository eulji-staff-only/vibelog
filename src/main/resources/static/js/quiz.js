/*
 * quiz.js — 퀴즈 화면(U-14, #54)의 화면 안 동작만 담당한다. 서버를 부르지 않는다 (화면 선행 작업).
 *
 * - 보기 선택 → "확인" → 정답·해설 펼침 → "다음". 확인한 문항은 답을 바꿀 수 없다 (PL-LSN-006).
 * - 낚시 문제(TRICK)는 물고기를 누르면 확인 버튼 없이 바로 확인한다 (설계서 U-14).
 * - 마지막 문항 뒤 "결과 보기" → 채점 결과를 같은 화면에 보여준다.
 *
 * 지금 정답(data-answer)·태그(data-tag)는 샘플 마크업에 들어 있고, 채점도 여기서 샘플로 계산한다.
 * ③ 단계(#108, #109)에서는 문항을 API-LSN-04 로 받고, 확인은 API-LSN-05, 채점 결과는 API-LSN-06 응답
 * (correctRate, isPassed, tagStats[].weak)을 그대로 그린다. 그때 이 계산 코드는 지운다.
 */
(function () {
    'use strict';

    const PASS_RATE = 50;      // PL-LSN-002 정답률 50% 이상이면 통과 (샘플 계산용)
    const WEAK_RATE = 60;      // PL-LSN-008 개념별 정답률 60% 미만이면 약한 개념 (샘플 계산용)

    const root = document.querySelector('[data-quiz]');
    if (!root) return;

    const solve = root.querySelector('[data-quiz-solve]');
    const result = root.querySelector('[data-quiz-result]');
    const questions = Array.from(root.querySelectorAll('[data-quiz-question]'));
    const checkBtn = root.querySelector('[data-quiz-check]');
    const nextBtn = root.querySelector('[data-quiz-next]');
    const reason = root.querySelector('[data-quiz-reason]');
    const counter = root.querySelector('[data-quiz-counter]');
    const progress = root.querySelector('[data-quiz-progress]');
    const progressFill = root.querySelector('[data-quiz-progress-fill]');
    const total = questions.length;

    let index = 0;
    let picked = {};      // 문항 번호 → 고른 보기 번호
    let checked = {};     // 문항 번호 → 확인했는지

    const choicesOf = (q) => Array.from(q.querySelectorAll('[data-choice]'));
    const isTrick = (q) => q.dataset.type === 'TRICK';

    function render() {
        questions.forEach((q, i) => { q.hidden = i !== index; });
        const q = questions[index];
        const done = Object.keys(checked).length;
        const isChecked = !!checked[index];

        counter.textContent = (index + 1) + ' / ' + total;
        progress.setAttribute('aria-valuenow', String(done));
        progressFill.style.width = Math.round(done / total * 100) + '%';

        // 낚시 문제는 확인 버튼이 없다
        checkBtn.hidden = isChecked || isTrick(q);
        reason.hidden = isChecked || isTrick(q) || picked[index] != null;
        checkBtn.setAttribute('aria-disabled', picked[index] == null ? 'true' : 'false');
        nextBtn.hidden = !isChecked;
        nextBtn.textContent = index === total - 1 ? '결과 보기' : '다음';
    }

    function pick(i) {
        const q = questions[index];
        if (checked[index]) return;
        picked[index] = i;
        choicesOf(q).forEach((c) => c.setAttribute('aria-pressed', String(Number(c.dataset.choice) === i)));

        const blank = q.querySelector('[data-quiz-blank]');
        if (blank) {
            blank.textContent = choicesOf(q)[i].textContent;
            blank.classList.add('is-filled');
        }
        if (isTrick(q)) {
            check();
            return;
        }
        render();
    }

    function check() {
        const q = questions[index];
        if (picked[index] == null || checked[index]) return;
        checked[index] = true;

        const answer = Number(q.dataset.answer);
        const correct = picked[index] === answer;
        choicesOf(q).forEach((c) => {
            const n = Number(c.dataset.choice);
            c.disabled = true;
            if (n === answer) c.classList.add('is-correct');
            else if (n === picked[index]) c.classList.add('is-wrong');
            if (n === answer && correct && isTrick(q)) c.classList.add('is-caught');
        });

        const feedback = q.querySelector('[data-quiz-feedback]');
        feedback.hidden = false;
        feedback.classList.toggle('is-correct', correct);
        feedback.classList.toggle('is-wrong', !correct);
        feedback.querySelector('[data-quiz-feedback-title]').textContent = correct ? '정답입니다!' : '아쉬워요, 오답입니다.';

        render();
        nextBtn.focus();
    }

    function next() {
        if (index < total - 1) {
            index += 1;
            render();
            questions[index].querySelector('.qz-q').setAttribute('tabindex', '-1');
            questions[index].querySelector('.qz-q').focus();
        } else {
            showResult();
        }
    }

    function showResult() {
        const tags = new Map();   // 태그 → { total, wrong }
        let correctCount = 0;
        questions.forEach((q, i) => {
            const ok = picked[i] === Number(q.dataset.answer);
            if (ok) correctCount += 1;
            const t = tags.get(q.dataset.tag) || { total: 0, wrong: 0 };
            t.total += 1;
            if (!ok) t.wrong += 1;
            tags.set(q.dataset.tag, t);
        });

        const rate = Math.round(correctCount / total * 100);
        const passed = rate >= PASS_RATE;
        root.querySelectorAll('[data-quiz-pass]').forEach((el) => { el.hidden = !passed; });
        root.querySelectorAll('[data-quiz-fail]').forEach((el) => { el.hidden = passed; });

        root.querySelector('[data-quiz-score]').setAttribute('aria-valuenow', String(rate));
        root.querySelector('[data-quiz-score-fill]').style.width = rate + '%';
        root.querySelector('[data-quiz-score-text]').textContent = '정답 ' + correctCount + ' / ' + total + ' · ' + rate + '%';

        const list = root.querySelector('[data-quiz-tags]');
        const template = list.querySelector('[data-quiz-tag-template]');
        list.querySelectorAll('[data-quiz-tag-row]').forEach((el) => el.remove());
        tags.forEach((t, name) => {
            const row = template.cloneNode(true);
            row.removeAttribute('data-quiz-tag-template');
            row.setAttribute('data-quiz-tag-row', '');
            row.hidden = false;
            const wrongRate = Math.round(t.wrong / t.total * 100);
            row.querySelector('[data-tag-name]').textContent = name;
            row.querySelector('[data-tag-text]').textContent = '오답 ' + t.wrong + ' / ' + t.total + ' · ' + wrongRate + '%';
            row.querySelector('[data-tag-fill]').style.width = wrongRate + '%';
            row.querySelector('[data-tag-weak]').hidden = (100 - wrongRate) >= WEAK_RATE;
            list.appendChild(row);
        });

        solve.hidden = true;
        result.hidden = false;
        result.querySelector('#qz-result-h').focus();
    }

    function reset() {
        index = 0;
        picked = {};
        checked = {};
        questions.forEach((q) => {
            choicesOf(q).forEach((c) => {
                c.disabled = false;
                c.setAttribute('aria-pressed', 'false');
                c.classList.remove('is-correct', 'is-wrong', 'is-caught');
            });
            q.querySelector('[data-quiz-feedback]').hidden = true;
            const blank = q.querySelector('[data-quiz-blank]');
            if (blank) {
                blank.textContent = '　';
                blank.classList.remove('is-filled');
            }
        });
        result.hidden = true;
        solve.hidden = false;
        render();
        questions[0].querySelector('.qz-q').setAttribute('tabindex', '-1');
        questions[0].querySelector('.qz-q').focus();
    }

    questions.forEach((q) => {
        choicesOf(q).forEach((c) => c.addEventListener('click', () => pick(Number(c.dataset.choice))));
    });
    checkBtn.addEventListener('click', () => {
        if (checkBtn.getAttribute('aria-disabled') === 'true') return;
        check();
    });
    nextBtn.addEventListener('click', next);
    root.querySelector('[data-quiz-retry]').addEventListener('click', reset);

    render();
})();

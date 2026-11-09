/*
 * diagnosis.js — 진단 설문 화면(U-07, #52·#99)의 문항 전환. 서버를 직접 부르지 않고 폼만 보낸다.
 *
 * - 문항을 하나씩 보여 준다. 보기를 고르면 그 문항의 숨은 입력(diagnosisOptionIds)에 보기 ID 를 넣고 "다음"을 연다.
 *   "이전"으로 돌아가도 고른 보기가 남아 있다.
 * - 마지막 문항에서는 "다음" 대신 "진단 완료"(제출 버튼)를 보여 준다. 고른 보기 ID 목록을 한 번에
 *   POST /diagnosis 로 보낸다 (API-DGN-02). 응답은 서버가 저장하지 않는다 (PL-DGN-001).
 * - M-01 건너뛰기 확인 모달은 공통 ui.js 가 여닫는다. "건너뛰기"를 누르면 POST /diagnosis/skip 폼을 보낸다 (API-DGN-03).
 */
(function () {
    'use strict';

    const root = document.querySelector('[data-diagnosis]');
    if (!root) return;

    const questions = Array.from(root.querySelectorAll('[data-dg-question]'));
    const prevBtn = root.querySelector('[data-dg-prev]');
    const nextBtn = root.querySelector('[data-dg-next]');
    const doneBtn = root.querySelector('[data-dg-done]');   // 제출 버튼
    const reason = root.querySelector('[data-dg-reason]');
    const progress = root.querySelector('[data-dg-progress]');
    const progressText = root.querySelector('[data-dg-progress-text]');
    const progressFill = root.querySelector('[data-dg-progress-fill]');
    const total = questions.length;
    if (total === 0) return;   // 사용 중인 문항이 없으면 화면이 안내 상자만 보여준다

    let index = 0;
    const picked = {};   // 문항 번호 → 고른 보기 번호

    function render(moveFocus) {
        questions.forEach((q, i) => { q.hidden = i !== index; });
        const isLast = index === total - 1;
        const hasPick = picked[index] != null;

        progress.setAttribute('aria-valuenow', String(index + 1));
        progressText.textContent = '문항 ' + (index + 1) + ' / ' + total;
        progressFill.style.width = Math.round((index + 1) / total * 100) + '%';

        prevBtn.setAttribute('aria-disabled', index === 0 ? 'true' : 'false');
        nextBtn.hidden = isLast;
        doneBtn.hidden = !isLast;
        nextBtn.setAttribute('aria-disabled', hasPick ? 'false' : 'true');
        doneBtn.setAttribute('aria-disabled', hasPick ? 'false' : 'true');
        reason.hidden = hasPick;
        reason.textContent = isLast ? '보기를 고르면 진단을 마칠 수 있습니다' : '보기를 고르면 다음으로 갈 수 있습니다';

        if (moveFocus) questions[index].querySelector('.dg-q').focus();
    }

    questions.forEach((q, qi) => {
        const choices = Array.from(q.querySelectorAll('[data-dg-choice]'));
        // 이 문항의 답. 고르기 전에는 disabled 라 제출되지 않는다 (서버가 문항 수와 답 수를 비교한다)
        const answer = q.querySelector('[data-dg-answer]');
        choices.forEach((c, ci) => {
            c.addEventListener('click', () => {
                picked[qi] = ci;
                choices.forEach((other) => other.setAttribute('aria-pressed', String(other === c)));
                answer.value = c.dataset.optionId;
                answer.disabled = false;
                render(false);
            });
        });
    });

    prevBtn.addEventListener('click', () => {
        if (prevBtn.getAttribute('aria-disabled') === 'true') return;
        index -= 1;
        render(true);
    });
    nextBtn.addEventListener('click', () => {
        if (nextBtn.getAttribute('aria-disabled') === 'true') return;
        index += 1;
        render(true);
    });
    // "진단 완료"는 제출 버튼이다. 모든 문항에 답하기 전에는 막는다
    root.querySelector('[data-dg-form]').addEventListener('submit', (e) => {
        if (Object.keys(picked).length !== total) e.preventDefault();
    });

    // M-01 "건너뛰기" (공통 confirmModal 의 확인 버튼) → 건너뜀 저장 폼(POST /diagnosis/skip)을 보낸다
    const skipConfirm = document.querySelector('#dg-skip-modal .vl-ui-btn.is-navy');
    const skipForm = document.querySelector('[data-dg-skip-form]');
    if (skipConfirm && skipForm) {
        skipConfirm.addEventListener('click', () => skipForm.submit());
    }

    render(false);
})();

/*
 * diagnosis.js — 진단 설문 화면(U-07, #52)의 화면 안 동작만 담당한다. 서버를 부르지 않는다 (화면 선행 작업).
 *
 * - 문항을 하나씩 보여 준다. 보기를 고르면 "다음"이 열리고, "이전"으로 돌아가면 고른 보기가 남아 있다.
 * - 마지막 문항에서는 "다음" 대신 "진단 완료"를 보여 준다. 지금은 결과 화면(U-08)으로 가는 링크이고,
 *   ③ 단계(#99)에서 고른 보기 ID 목록을 한 번에 제출하는 폼으로 바꾼다. 응답은 저장하지 않는다 (PL-DGN-001).
 * - M-01 건너뛰기 확인 모달은 공통 ui.js 가 여닫는다. 여기서는 "건너뛰기"를 누르면 코스 목록(U-10)으로만 보낸다.
 *   ③ 단계에서 건너뜀 저장 후 이동하는 폼으로 바꾼다 (FR-DGN-003).
 */
(function () {
    'use strict';

    const root = document.querySelector('[data-diagnosis]');
    if (!root) return;

    const questions = Array.from(root.querySelectorAll('[data-dg-question]'));
    const prevBtn = root.querySelector('[data-dg-prev]');
    const nextBtn = root.querySelector('[data-dg-next]');
    const doneBtn = root.querySelector('[data-dg-done]');
    const reason = root.querySelector('[data-dg-reason]');
    const progress = root.querySelector('[data-dg-progress]');
    const progressText = root.querySelector('[data-dg-progress-text]');
    const progressFill = root.querySelector('[data-dg-progress-fill]');
    const total = questions.length;

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
        choices.forEach((c, ci) => {
            c.addEventListener('click', () => {
                picked[qi] = ci;
                choices.forEach((other) => other.setAttribute('aria-pressed', String(other === c)));
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
    doneBtn.addEventListener('click', (e) => {
        if (doneBtn.getAttribute('aria-disabled') === 'true') e.preventDefault();
    });

    // M-01 "건너뛰기" (공통 confirmModal 의 확인 버튼)
    const skipConfirm = document.querySelector('#dg-skip-modal .vl-ui-btn.is-navy');
    if (skipConfirm) {
        skipConfirm.addEventListener('click', () => { window.location.href = '/courses'; });
    }

    render(false);
})();

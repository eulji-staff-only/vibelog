/*
 * 포트폴리오 화면(U-17) 안에서만 도는 동작: 스텝 완료 수, 글자 수 표시, 입력 확인, 공개 여부 선택, 제출 버튼 켜고 끄기.
 * 서버를 부르지 않는다. 스텝 체크 저장(API-PRT-02)과 제출(API-PRT-04)은 ③ 단계에서 붙인다.
 * 여기 확인은 안내용이고, 실제 검증은 ③ 단계에서 서버가 한다 (NFR-SEC-004).
 */
document.addEventListener('DOMContentLoaded', () => {
    const root = document.querySelector('[data-pf]');
    if (!root) return;

    // 스텝 가이드: n / 전체 완료
    const steps = [...root.querySelectorAll('[data-step]')];
    const renderSteps = () => {
        root.querySelector('[data-step-done]').textContent = steps.filter((s) => s.checked).length;
        root.querySelector('[data-step-total]').textContent = steps.length;
    };
    steps.forEach((s) => s.addEventListener('change', renderSteps));
    renderSteps();

    // 제출 양식
    const url = root.querySelector('[data-url]');
    const urlMsg = root.querySelector('[data-url-msg]');
    const urlHint = urlMsg.textContent;
    const retro = root.querySelector('[data-retro]');
    const retroMsg = root.querySelector('[data-retro-msg]');
    const required = [...root.querySelectorAll('[data-required]')];
    const visButtons = [...root.querySelectorAll('[data-vis]')];
    const submit = root.querySelector('[data-submit]');

    const isUrl = (value) => /^https?:\/\/[^\s]+\.[^\s]+$/.test(value.trim());
    const retroOk = () => retro.value.length >= 100 && retro.value.length <= 2000;

    const showCount = (field) => {
        const count = field.closest('.pf-field').querySelector('[data-count]');
        if (count) count.textContent = field.value.length;
    };

    const render = () => {
        // 링크 형식 (FR-PRT-004): 입력한 뒤에만 오류를 보여 준다
        const urlBad = url.value.length > 0 && !isUrl(url.value);
        url.setAttribute('aria-invalid', String(urlBad));
        urlMsg.textContent = urlBad ? '올바른 URL을 입력하세요' : urlHint;
        urlMsg.classList.toggle('is-error', urlBad);

        // 회고 길이 (PL-PRT-003)
        const retroBad = retro.value.length > 0 && !retroOk();
        retro.setAttribute('aria-invalid', String(retroBad));
        retroMsg.classList.toggle('is-error', retroBad);

        const ready = isUrl(url.value)
            && required.every((f) => f.value.trim().length > 0 && f.value.length <= Number(f.dataset.max || Infinity))
            && retroOk()
            && visButtons.some((b) => b.getAttribute('aria-pressed') === 'true');
        submit.setAttribute('aria-disabled', String(!ready));
    };

    [url, retro, ...required].forEach((field) => field.addEventListener('input', () => {
        showCount(field);
        render();
    }));

    // 공개 여부 (PL-PRT-006): 기본값 없이 하나만 고른다
    visButtons.forEach((button) => button.addEventListener('click', () => {
        visButtons.forEach((b) => b.setAttribute('aria-pressed', String(b === button)));
        render();
    }));

    render();
});

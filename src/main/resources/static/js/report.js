/*
 * M-11 신고 (API-RPT-01, #255). message/report.html 조각과 같이 쓴다.
 * - 신고 버튼(data-report-type·data-report-id)을 누르면 그 대상을 폼에 넣는다. 모달 열기는 ui.js 가 한다.
 * - 보내면 JSON 으로 접수한다. 204 면 모달을 닫고 토스트, 실패면 서버가 준 문구를 모달 안에 보여 준다.
 */
(function () {
    const form = document.querySelector('[data-report-form]');
    if (!form) return;
    const error = form.querySelector('[data-report-error]');
    const submit = form.querySelector('button[type="submit"]');
    // 모달을 새로 열 때마다 올린다. 이전 신고의 응답이 늦게 와도 새로 연 모달은 바꾸지 않는다.
    let generation = 0;

    document.addEventListener('click', (e) => {
        const button = e.target.closest('[data-report-type]');
        if (!button) return;
        generation++;
        submit.disabled = false;
        form.reset();
        error.textContent = '';
        form.elements.targetType.value = button.dataset.reportType;
        form.elements.targetId.value = button.dataset.reportId;
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        // 응답 전에 다시 보내면 사유가 바뀐 줄 알 수 있어 요청 중에는 버튼을 막는다
        if (submit.disabled) return;
        submit.disabled = true;
        const sent = generation;
        error.textContent = '';
        const detail = form.elements.reasonDetail.value.trim();
        const body = {
            targetType: form.elements.targetType.value,
            targetId: Number(form.elements.targetId.value),
            reason: form.elements.reason.value || null,
            reasonDetail: detail || null,
        };
        try {
            const res = await fetch(form.dataset.url, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', [form.dataset.csrfHeader]: form.dataset.csrfToken },
                body: JSON.stringify(body),
            });
            if (sent !== generation) return;
            if (res.ok) {
                form.closest('[data-ui-modal]').querySelector('[data-ui-modal-close]').click();
                window.VLToast.show('신고가 접수되었습니다');
                return;
            }
            const data = await res.json().catch(() => ({}));
            if (sent !== generation) return;
            error.textContent = data.message || '신고하지 못했습니다. 잠시 뒤 다시 시도해 주세요.';
        } catch (err) {
            if (sent !== generation) return;
            error.textContent = '신고하지 못했습니다. 잠시 뒤 다시 시도해 주세요.';
        } finally {
            if (sent === generation) submit.disabled = false;
        }
    });
})();

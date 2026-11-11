/*
 * 포트폴리오 화면(U-17) 동작: 스텝 체크 자동 저장(API-PRT-02), 글자 수 표시, 입력 확인, 공개 여부 선택, 제출 버튼 켜고 끄기.
 * 제출(API-PRT-04)은 #113 에서 붙인다. 여기 확인은 안내용이고, 실제 검증은 서버가 한다 (NFR-SEC-004).
 */
document.addEventListener('DOMContentLoaded', () => {
    // 이미 제출한 화면이면 보관해 둔 작성 내용을 지운다 (FR-PRT-010). 저장소를 못 쓰는 브라우저면 넘어간다
    document.querySelectorAll('[data-draft-clear]').forEach((el) => {
        try { localStorage.removeItem(el.dataset.draftClear); } catch (e) { /* 저장소 사용 불가 */ }
    });

    const root = document.querySelector('[data-pf]');
    if (!root) return;

    // 스텝 가이드: 체크하면 바로 저장하고, 서버가 돌려준 "n / 전체"로 고친다.
    // 저장 요청은 한 줄로 세워 앞 요청이 끝난 뒤 보낸다. 빠르게 체크·해제해도 마지막 선택이 마지막으로 저장된다.
    const csrfToken = document.querySelector('meta[name="_csrf"]').content;
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]').content;
    const stepError = root.querySelector('[data-step-error]');
    let saving = Promise.resolve();
    const save = (step, checked) => fetch(step.dataset.checkUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', [csrfHeader]: csrfToken },
        body: JSON.stringify({ checked }),
    })
        .then((res) => res.json().then((body) => {
            if (!res.ok) throw body;   // 서버 오류 {code, message}
            return body;
        }))
        .then((body) => {
            root.querySelector('[data-step-done]').textContent = body.checkedCount;
            root.querySelector('[data-step-total]').textContent = body.totalCount;
            stepError.hidden = true;
        })
        .catch((err) => {
            // 그 사이 다시 바꿨으면 이후 선택을 되돌리지 않는다
            if (step.checked === checked) step.checked = !checked;
            // 네트워크 끊김 같은 브라우저 오류(Error)는 기술 문구라 보여 주지 않는다 (PL-ERR-001)
            stepError.textContent = err instanceof Error || !err.message
                ? '저장하지 못했습니다. 잠시 후 다시 시도해 주세요.' : err.message;
            stepError.hidden = false;
        });
    root.querySelectorAll('[data-step]').forEach((step) => step.addEventListener('change', () => {
        const checked = step.checked;
        saving = saving.then(() => save(step, checked));
    }));

    // 제출 양식 (API-PRT-04): 이미 제출했거나 이메일 미인증이면 양식이 없다
    const form = root.querySelector('[data-submit-form]');
    const url = root.querySelector('[data-url]');
    if (!url) return;
    const urlMsg = root.querySelector('[data-url-msg]');
    const urlHint = urlMsg.textContent;
    const retro = root.querySelector('[data-retro]');
    const retroMsg = root.querySelector('[data-retro-msg]');
    const required = [...root.querySelectorAll('[data-required]')];
    const visButtons = [...root.querySelectorAll('[data-vis]')];
    const visInput = root.querySelector('[data-vis-input]');
    const submit = root.querySelector('[data-submit]');
    const keyFields = [...root.querySelectorAll('[data-key-check]')];
    const keyWarning = root.querySelector('[data-key-warning]');
    const shot = root.querySelector('[data-shot]');
    const shotError = root.querySelector('[data-shot-error]');
    const SHOT_MAX = 5 * 1024 * 1024;   // PL-PRT-005·PL-DAT-004. 서버(FileStorage)도 다시 검사한다
    // U-34 개인 작품 양식에는 스크린샷 칸이 없다 (#199)
    const shotOk = () => !shot || !shot.files.length || shot.files[0].size <= SHOT_MAX;
    // 서버(ApiKeyMasker)와 같은 형식 (PL-PRT-008)
    const apiKey = /(^|[^A-Za-z0-9])(sk-|sk_test_|sk_live_|ghp_|github_pat_|AKIA|AIza)[A-Za-z0-9_-]+/;
    let ready = false;

    // 작성 중 내용 보존 (FR-PRT-010, PL-ERR-003·004·005): 입력할 때마다와 제출을 누를 때 입력 값을 회원·코스별 키로
    // 7일 보관하고, 다시 열면 채운다. 그래서 서버 오류·네트워크 끊김·세션 만료(제출 전 포함)로 제출이 안 돼도 내용이 남는다.
    // 새 제출과 수정(edit=true)은 서버가 다른 키를 준다.
    // 보관 전에 API 키 형식은 서버와 같은 규칙으로 가린다(PL-PRT-008). 스크린샷 파일은 보관하지 않는다.
    // 제출 성공이 확인되면(이미 제출함 화면, 수정 후 내 포트폴리오) data-draft-clear 로 지운다.
    const DRAFT_MAX_AGE = 7 * 24 * 60 * 60 * 1000;   // PL-ERR-004 7일
    const draftKey = form.dataset.draftKey;
    // 양식에 있는 칸만 보관한다 (U-34 개인 작품에는 에이전트 점검 결과가 없다)
    const draftFields = ['resultUrl', 'title', 'summary', 'retrospective', 'aiTools', 'agentResult'].filter((name) => form.elements[name]);
    const maskKeys = (value) => value.replace(/(?<![A-Za-z0-9])(sk-|sk_test_|sk_live_|ghp_|github_pat_|AKIA|AIza)[A-Za-z0-9_-]+/g, '$1****');
    const readDraft = () => {
        try {
            const draft = JSON.parse(localStorage.getItem(draftKey));
            if (draft && Date.now() - draft.savedAt <= DRAFT_MAX_AGE) return draft;
            localStorage.removeItem(draftKey);   // 없거나 7일이 지났다
        } catch (e) { /* 저장소 사용 불가 · 깨진 값 */ }
        return null;
    };
    const saveDraft = () => {
        const draft = { savedAt: Date.now(), isPublic: visInput.value };
        draftFields.forEach((name) => { draft[name] = maskKeys(form.elements[name].value); });
        try { localStorage.setItem(draftKey, JSON.stringify(draft)); } catch (e) { /* 저장소 사용 불가 · 가득 참 */ }
    };
    const draft = readDraft();
    if (draft) {
        draftFields.forEach((name) => { if (typeof draft[name] === 'string') form.elements[name].value = draft[name]; });
        if (draft.isPublic === 'true' || draft.isPublic === 'false') {
            visInput.value = draft.isPublic;
            visButtons.forEach((b) => b.setAttribute('aria-pressed', String(b.dataset.visValue === draft.isPublic)));
        }
        root.querySelector('[data-draft-notice]').hidden = false;
    }

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

        // API 키 형식 경고 (PL-PRT-008): 저장할 때 서버가 가린다
        keyWarning.hidden = !keyFields.some((f) => apiKey.test(f.value));

        // 스크린샷 크기: 너무 크면 서버 업로드 제한에 걸려 입력한 내용이 사라지므로 보내기 전에 막는다
        if (shotError) shotError.hidden = shotOk();

        ready = shotOk() && isUrl(url.value)
            && required.every((f) => f.value.trim().length > 0 && f.value.length <= Number(f.dataset.max || Infinity))
            && retroOk()
            && visButtons.some((b) => b.getAttribute('aria-pressed') === 'true');
        submit.setAttribute('aria-disabled', String(!ready));
    };

    [url, retro, ...required, ...keyFields].forEach((field) => field.addEventListener('input', () => {
        showCount(field);
        render();
        saveDraft();
    }));

    // 공개 여부 (PL-PRT-006): 기본값 없이 하나만 고른다. 고른 값을 숨은 칸 isPublic 에 넣는다
    visButtons.forEach((button) => button.addEventListener('click', () => {
        visButtons.forEach((b) => b.setAttribute('aria-pressed', String(b === button)));
        visInput.value = button.dataset.visValue;
        render();
        saveDraft();
    }));

    // 준비가 안 됐으면 보내지 않고, 보낼 때는 버튼을 잠가 중복 제출을 막는다 (PL-ERR-003)
    if (shot) shot.addEventListener('change', render);

    form.addEventListener('submit', (e) => {
        if (!ready || submit.disabled) {
            e.preventDefault();
            return;
        }
        saveDraft();
        submit.disabled = true;
    });

    // 입력 오류로 다시 그린 폼은 보낸 값이 채워져 있어 글자 수부터 맞춘다
    [url, retro, ...required].forEach(showCount);
    render();
});

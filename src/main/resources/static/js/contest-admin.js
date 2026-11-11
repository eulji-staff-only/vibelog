/*
 * contest-admin.js — A-11 콘테스트 등록·수정 폼. 서버를 부르지 않는다.
 * 월을 바꾸면 출품·투표 기간 4칸을 그 달 기본값으로 바꾼다 (#401). 모든 기간은 그 달 안이어야 해서(contest 명세 결정 15)
 * 월만 바꾸면 매번 "그 달 안이어야 합니다" 오류가 났다.
 * 기본값은 서버 ContestForm.defaultsFor 와 같다 (PL-CTS-001): 1일 0시 / 21일 0시 / 21일 0시 / 다음 달 1일 0시.
 */
document.addEventListener('DOMContentLoaded', () => {
    const month = document.getElementById('contestMonth');
    if (!month) return;

    month.addEventListener('change', () => {
        if (!/^\d{4}-\d{2}$/.test(month.value)) return;
        const [year, mon] = month.value.split('-').map(Number);
        const next = mon === 12 ? `${year + 1}-01` : `${year}-${String(mon + 1).padStart(2, '0')}`;
        const set = (id, value) => { document.getElementById(id).value = value; };
        set('entryStartsAt', `${month.value}-01T00:00`);
        set('entryEndsAt', `${month.value}-21T00:00`);
        set('voteStartsAt', `${month.value}-21T00:00`);
        set('voteEndsAt', `${next}-01T00:00`);
    });
});

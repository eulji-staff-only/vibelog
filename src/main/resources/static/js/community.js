/*
 * 커뮤니티 화면(U-22·U-23) 안에서만 도는 동작: 삭제 확인 모달(M-05) 열기, 제목·본문 글자 수 표시 (PL-CMT-010).
 * 서버를 부르지 않는다. 모달 닫기는 dialog 안 form method="dialog" 버튼이 한다.
 * 공통 UI 조각(#46)의 모달이 머지되면 그것으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-open-dialog]').forEach((button) => {
        button.addEventListener('click', () => document.getElementById(button.dataset.openDialog).showModal());
    });

    document.querySelectorAll('[data-count]').forEach((input) => {
        const count = document.querySelector(`[data-count-for="${input.id}"]`);
        const render = () => { count.textContent = input.value.length.toLocaleString(); };
        input.addEventListener('input', render);
        render();
    });
});

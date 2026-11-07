/*
 * 회원 화면(U-24) 안에서만 도는 동작: 회원 탈퇴 확인 모달(M-06) 열기. 서버를 부르지 않는다.
 * 모달 닫기는 dialog 안 form method="dialog" 버튼이 한다. 공통 UI 조각(#46)의 모달이 머지되면 그것으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-open-dialog]').forEach((button) => {
        button.addEventListener('click', () => document.getElementById(button.dataset.openDialog).showModal());
    });
});

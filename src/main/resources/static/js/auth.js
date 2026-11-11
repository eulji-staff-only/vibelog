/*
 * 회원 화면 안에서만 도는 동작. 서버를 부르지 않는다.
 * - U-24: 회원 탈퇴 확인 모달(M-06) 열기.
 * - U-03: 로그인 잠금 남은 시간(분:초)을 1초마다 줄이고, 0이 되면 다시 로그인할 수 있다고 알린다 (#394).
 * 모달 닫기는 dialog 안 form method="dialog" 버튼이 한다. 공통 UI 조각(#46)의 모달이 머지되면 그것으로 바꾼다.
 */
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('[data-open-dialog]').forEach((button) => {
        button.addEventListener('click', () => document.getElementById(button.dataset.openDialog).showModal());
    });

    const lockWait = document.querySelector('[data-lock-wait]');
    if (lockWait) {
        // 탭이 백그라운드면 setInterval 이 늦어지므로 시작 시각 기준으로 남은 시간을 다시 계산한다
        const endsAt = Date.now() + Number(lockWait.dataset.lockSeconds) * 1000;
        const left = lockWait.querySelector('[data-lock-left]');
        const tick = () => {
            const seconds = Math.max(0, Math.ceil((endsAt - Date.now()) / 1000));
            left.textContent = Math.floor(seconds / 60) + ':' + String(seconds % 60).padStart(2, '0');
            if (seconds === 0) {
                clearInterval(timer);
                lockWait.hidden = true;
                document.querySelector('[data-lock-done]').hidden = false;
            }
        };
        const timer = setInterval(tick, 1000);
    }
});

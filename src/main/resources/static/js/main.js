/*
 * main.js
 * U-01 메인 화면의 세그먼트 탭 전환. 서버를 부르지 않고 화면 안에서만 탭(패널)을 바꾼다.
 */
document.addEventListener("DOMContentLoaded", function () {
  var tabs = document.querySelectorAll(".vl-tab");

  tabs.forEach(function (tab) {
    tab.addEventListener("click", function () {
      var targetId = tab.getAttribute("data-target");

      tabs.forEach(function (other) {
        other.classList.toggle("is-active", other === tab);
      });

      document.querySelectorAll(".vl-panel").forEach(function (panel) {
        panel.hidden = panel.id !== targetId;
      });
    });
  });
});

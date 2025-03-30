// 햄버거 버튼
function toggleMenu() {
    const hamburger = document.querySelector('.hamburger');
    const menu = document.querySelector('.menu');

    if (!hamburger || !menu) return;

    hamburger.classList.toggle('open');
    menu.classList.toggle('open');
}

// 메뉴 외부 클릭 시 닫기
document.addEventListener("click", function (event) {
    const hamburger = document.querySelector('.hamburger');
    const menu = document.querySelector('.menu');

    if (!menu || !hamburger) return; // 요소가 없으면 실행하지 않음

    if (menu.classList.contains('open') && !menu.contains(event.target) && !hamburger.contains(event.target)) {
        menu.classList.remove('open');
        hamburger.classList.remove('open');
    }
});
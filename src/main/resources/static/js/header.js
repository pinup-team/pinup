// 햄버거 버튼
function toggleMenu() {
    const hamburger = document.querySelector('.hamburger');
    const menu = document.querySelector('.menu');

    hamburger.classList.toggle('open');
    menu.classList.toggle('open');
}

// 메뉴 외부 클릭 시 닫기
document.addEventListener("click", function (event) {
    const hamburger = document.querySelector('.hamburger');
    const menu = document.getElementById("menu");

    if (menu.classList.contains('open') && !menu.contains(event.target) && !hamburger.contains(event.target)) {
        menu.classList.remove('open');
    }
});
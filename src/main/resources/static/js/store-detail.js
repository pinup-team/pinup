document.addEventListener("DOMContentLoaded", function () {
    function changeTab(tab) {
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`tab-${tab}`).classList.add('active');
    }

    window.changeTab = changeTab;
});

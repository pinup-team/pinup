document.addEventListener("DOMContentLoaded", function () {
    const statusFilter = document.getElementById("statusFilter");
    const sigunguFilter = document.getElementById('sigungu-filter');

    const urlParams = new URLSearchParams(window.location.search);
    if (statusFilter && urlParams.has('status')) {
        statusFilter.value = urlParams.get('status');
    }
    if (sigunguFilter && urlParams.has('sigungu')) {
        sigunguFilter.value = urlParams.get('sigungu');
    }

    if (statusFilter) {
        statusFilter.addEventListener("change", function () {
            this.form.submit();
        });
    }
    if (sigunguFilter) {
        sigunguFilter.addEventListener('change', function () {
            this.form.submit();
        });
    }
});
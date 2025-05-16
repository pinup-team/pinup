
document.addEventListener("DOMContentLoaded", function () {
    const statusFilter = document.getElementById("statusFilter");
    if (statusFilter) {
        statusFilter.addEventListener("change", function () {
            this.form.submit();
        });
    }
})
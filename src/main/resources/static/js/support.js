document.addEventListener('DOMContentLoaded', () => {
    const tabButtons = document.querySelectorAll('[role="tab"]');

    const setActiveTab = (activeTab) => {
        tabButtons.forEach((btn) => btn.setAttribute("aria-selected", "false"));
        activeTab.setAttribute('aria-selected', 'true');
    };

    const savedTab = localStorage.getItem('activeTab');
    const initialTab = savedTab ? document.querySelector(`[role="tab"].${savedTab}`) : tabButtons[0];

    setActiveTab(initialTab);

    if (!savedTab) {
        localStorage.setItem('activeTab', initialTab.className);
    }

    tabButtons.forEach((button) => {
        button.addEventListener('click', (evt) => {
            const target = evt.currentTarget;
            setActiveTab(target);
            localStorage.setItem('activeTab', target.className);
        });
    });
});
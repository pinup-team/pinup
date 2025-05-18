document.addEventListener('DOMContentLoaded', () => {
    const tabs = document.querySelectorAll('[role="tab"]');

    const currentPath = window.location.pathname;
    const domain = currentPath.match(/^\/([^\/]+)/)[1];

    const setActiveTab = (activeTab) => {
        tabs.forEach((btn) => btn.setAttribute("aria-selected", "false"));
        activeTab.setAttribute('aria-selected', 'true');
    };

    const selectedTabClass = domain || 'notices';
    const initialTab = document.querySelector(`[role="tab"].${selectedTabClass}`) || tabs[0];

    setActiveTab(initialTab);

    tabs.forEach((tab) => {
        tab.addEventListener('click', (event) => {
            const target = event.currentTarget;
            setActiveTab(target);
        });
    });
});

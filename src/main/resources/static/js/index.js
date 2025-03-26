async function fetchStoreSummaries() {
    try {
        const response = await fetch('/api/stores');

        if (!response.ok) {
            throw new Error('네트워크 응답이 올바르지 않습니다.');
        }

        const stores = await response.json();
        const storeListElement = document.getElementById('store-list');

        const topStores = stores.slice(0, 5);

        // 리스트에 팝업스토어 추가
        topStores.forEach(store => {
            const listItem = document.createElement('li');
            listItem.innerHTML = `<div class="store-card">
                                    <img src="${store.imageUrl}" alt="Loading Image"
                                        onerror="this.onerror=null; this.src='/images/loading.png';">
                                    <div class="list-store-info">
                                        <div class="list-store-info-head">
                                            <h3>${store.name}</h3>
                                            <span>📍${store.location.district}</span>
                                        </div>
                                        <span class="list-store-info-date">~ ${store.endDate}</span>
                                    </div>
                                  </div>`;

            listItem.querySelector('.store-card').addEventListener('click', () => {
                window.location.href = `/stores/${store.id}`;
            });
            storeListElement.appendChild(listItem);
        });
    } catch (error) {
        console.error('팝업스토어 목록을 가져오는 중 오류 발생:', error);
    }
}

// 페이지가 로드되면 팝업스토어 목록을 가져옴
document.addEventListener('DOMContentLoaded', fetchStoreSummaries);
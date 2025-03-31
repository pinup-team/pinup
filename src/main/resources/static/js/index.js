// 배너 슬라이드
document.addEventListener("DOMContentLoaded", function() {
    let currentIndex = 0;
    const totalSlides = document.querySelectorAll('.carousel-item').length;
    const carouselList = document.querySelector('.carousel-list');

    // 슬라이드 이동 함수
    function moveToNextSlide() {
        currentIndex++;
        if (currentIndex >= totalSlides) {
            currentIndex = 0; // 마지막 슬라이드에서 첫 슬라이드로 돌아감
        }
        updateSlidePosition();
    }

    // 슬라이드 위치 업데이트 함수
    function updateSlidePosition() {
        const offset = -currentIndex * 100; // 각 슬라이드의 너비만큼 이동
        carouselList.style.transition = 'transform 0.5s ease-in-out'; // 부드럽게 이동하도록 transition 추가
        carouselList.style.transform = `translateX(${offset}%)`;
    }

    // 일정 시간 간격으로 자동 슬라이드 이동
    setInterval(moveToNextSlide, 3000); // 3초마다 슬라이드 이동

    // 슬라이드 이동 버튼 클릭 시
    document.querySelector('.slide-left').addEventListener('click', function() {
        currentIndex--;
        if (currentIndex < 0) {
            currentIndex = totalSlides - 1; // 첫 슬라이드에서 마지막 슬라이드로 돌아감
        }
        updateSlidePosition();
    });

    document.querySelector('.slide-right').addEventListener('click', function() {
        moveToNextSlide();
    });
});

// 스토어 리스트 가져오기
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

// 사용자 로그인 후 띄우는 alert
document.addEventListener("DOMContentLoaded", function() {
    const cookies = document.cookie.split('; ');
    const messageCookie = cookies.find(row => row.startsWith('loginMessage='));

    if (messageCookie) {
        let message = decodeURIComponent(messageCookie.split('=')[1]);
        message = message.replace(/\+/g, ' '); // '+'를 공백으로 변환

        alert(message);

        // 메시지를 한 번만 표시하도록 삭제
        document.cookie = "loginMessage=; path=/; max-age=0";
    }
});
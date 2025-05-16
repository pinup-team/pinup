
async function changeTab(tab) {
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    document.getElementById(`tab-${tab}`).classList.add('active');

    document.querySelectorAll('.tab-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelector(`.tab-item[data-tab="${tab}"]`).classList.add('active');

    if (tab === "info") {
        setTimeout(() => {
            if (typeof kakao !== "undefined") {
                loadMap();
            }
        }, 300);
        return;
    }

    if (tab === "post") {
        try {
            const storeId = document.getElementById("storeId").value;

            const response = await fetch(`/post/list/${storeId}`);

            if (!response.ok) {
                throw new Error(`게시판 데이터를 불러오지 못했습니다. (HTTP ${response.status})`);
            }

            const postHtml = await response.text();

            const postContainer = document.getElementById("post-list-container");
            postContainer.innerHTML = postHtml;
            postContainer.style.display = "block";

        } catch (error) {
            console.error("게시판 데이터 로딩 중 오류 발생:", error);
            alert("게시판 데이터를 불러오는 중 오류가 발생했습니다.");
        }
    }
}


document.addEventListener("DOMContentLoaded", function () {
    const imageList = document.querySelector(".image-list");
    const items = document.querySelectorAll(".image-item");
    const prevButton = document.querySelector(".slide-left");
    const nextButton = document.querySelector(".slide-right");

    if (items.length <= 1) {
        return;
    }

    const itemWidth = 500;
    let currentIndex = 0;
    const maxIndex = items.length - 2;

    function updateSlider() {
        const offset = currentIndex * itemWidth;
        imageList.style.transform = `translateX(-${offset}px)`;
    }

    nextButton.addEventListener("click", () => {
        if (currentIndex < maxIndex) {
            currentIndex++;
            updateSlider();
        }
    });

    prevButton.addEventListener("click", () => {
        if (currentIndex > 0) {
            currentIndex--;
            updateSlider();
        }
    });
});




// 카카오 지도 API 로드 함수
function loadMap() {
    var latitude = document.getElementById("latitude-hidden").value;
    var longitude = document.getElementById("longitude-hidden").value;
    var storeName = document.getElementById("store-name") ? document.getElementById("store-name").textContent.trim() : "매장 위치";
    var storeAddr = document.getElementById("store-address") ? document.getElementById("store-address").textContent.trim() : "";

    var mapContainer = document.getElementById("map");

    if (!latitude || !longitude || !mapContainer) {
        console.error("지도 정보를 불러올 수 없습니다.");
        return;
    }

    var mapOption = {
        center: new kakao.maps.LatLng(latitude, longitude),
        level: 8
    };

    var map = new kakao.maps.Map(mapContainer, mapOption);

    var marker = new kakao.maps.Marker({
        map: map,
        position: new kakao.maps.LatLng(latitude, longitude)
    });

    var infowindow = new kakao.maps.InfoWindow({
        content: `<div style="width:200px;text-align:center;padding:3px 0;">${storeName}</div>`
    });

    infowindow.open(map, marker);

    // 🟢 지도 크기 재조정 (탭 전환 시 지도가 정상적으로 보이도록)
    setTimeout(() => {
        map.relayout();
        map.setCenter(new kakao.maps.LatLng(latitude, longitude));
    }, 500);
}

// 페이지가 로드되면 initializeMap 함수 실행
document.addEventListener("DOMContentLoaded", function () {
    if (typeof kakao !== "undefined") {
        kakao.maps.load(loadMap);
    }
});
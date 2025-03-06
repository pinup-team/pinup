// 주소 검색 함수 (Kakao 주소 API)
function searchAddress() {
    new daum.Postcode({
        oncomplete: function (data) {
            // Kakao API로부터 받은 주소 데이터를 각각의 입력 필드에 할당
            document.getElementById("zoneCode").value = data.zonecode;
            document.getElementById("address").value = data.roadAddress;
            document.getElementById("addressDetail").value = '';

            // getCoordinates 함수 호출하여 위도와 경도 가져오기
            getCoordinates(data.roadAddress);

            // window.locationData 객체에 값 저장
            window.locationData = { state: data.sido, district: data.sigungu, zoneCode: data.zonecode, address: data.roadAddress };
        }
    }).open();
}
// 주소로부터 위도와 경도 얻기 (Kakao Geocoding API 사용)
async function getCoordinates(address) {
    const url = `https://dapi.kakao.com/v2/local/search/address.json`;
    const encodedAddress = encodeURIComponent(address);
    try {
        const response = await fetch(url + `?query=${encodedAddress}`, {
            method: 'GET',
            headers: {
                Authorization: 'KakaoAK eab583a70155a7704ae8947deae9b297',  // 실제 Kakao API Key를 넣어주세요
            },
        });

        if (!response.ok) {
            throw new Error('Kakao API 호출 실패');
        }

        const data = await response.json();
        const result = data.documents[0];

        if (result) {
            const latitude = result.y;
            const longitude = result.x;

            window.locationData.latitude = latitude;
            window.locationData.longitude = longitude;

        } else {
            console.error("위도와 경도를 찾을 수 없습니다.");
        }
    } catch (error) {
        console.error("Kakao API 호출 실패:", error);
    }
}

function registerLocation() {
    const zoneCode = document.getElementById("zoneCode").value;
    const address = document.getElementById("address").value;
    const addressDetail = document.getElementById("addressDetail").value;

    if (!zoneCode || !address || !window.locationData.latitude || !window.locationData.longitude) {
        alert("주소 검색을 먼저 진행해주세요.");
        return;
    }

    const requestData = {
        name: "등록된 주소",
        zoneCode: zoneCode,
        state: window.locationData.state,
        district: window.locationData.district,
        latitude: window.locationData.latitude,
        longitude: window.locationData.longitude,
        address: address,
        addressDetail: addressDetail
    };

    fetch("/api/locations", {
        method: "POST",
        headers: {
            "Content-Type": "application/json"
        },
        body: JSON.stringify(requestData)
    })
        .then(response => response.json())
        .then(data => {
            alert("주소가 등록되었습니다.");
            document.getElementById("storeForm").style.display = "block";
            document.querySelector("input[name='locationId']").value = data.id;
        })
        .catch(error => {
            console.error("Error:", error);
            alert("주소 등록 중 오류가 발생했습니다.");
        });
}

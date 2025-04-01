// 주소 검색 함수 (Kakao 주소 API)

const KAKAO_API_KEY = window.KAKAO_API_KEY;

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
                Authorization: `KakaoAK ${localStorage.getItem('KAKAO_API_KEY')}`,
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

async function registerLocation() {
    const zoneCode = document.getElementById("zoneCode").value;
    console.log("zoneCode", zoneCode);
    const address = document.getElementById("address").value;
    console.log("address", address);
    const addressDetail = document.getElementById("addressDetail").value;
    console.log("addressDetail", addressDetail);

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

    try {
        const response = await fetch("/api/locations", {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify(requestData)
        });

        if (!response.ok) {
            throw new Error("주소 등록 실패");
        }

        const data = await response.json();
        console.log("data", data);


        return data.id;
    } catch (error) {
        console.error("주소 등록 중 오류 발생");
        return null;
    }


}

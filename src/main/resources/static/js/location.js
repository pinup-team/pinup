// 주소 검색 함수 (Kakao 주소 API)

function searchAddress() {
    new daum.Postcode({
        oncomplete: function (data) {
            // Kakao API로부터 받은 주소 데이터를 각각의 입력 필드에 할당
            document.getElementById("zoneCode").value = data.zonecode;
            document.getElementById("address").value = data.roadAddress;
            document.getElementById("addressDetail").value = '';

         /*   // getCoordinates 함수 호출하여 위도와 경도 가져오기
            getCoordinates(data.roadAddress);*/

            // window.locationData 객체에 값 저장
            window.locationData = { state: data.sido, district: data.sigungu, zoneCode: data.zonecode, address: data.roadAddress };
        }
    }).open();
}

async function registerLocation() {
    const zoneCode = document.getElementById("zoneCode").value;
    console.log("zoneCode", zoneCode);
    const address = document.getElementById("address").value;
    console.log("address", address);
    const addressDetail = document.getElementById("addressDetail").value;
    console.log("addressDetail", addressDetail);

    const requestData = {
        name: "등록된 주소",
        zoneCode: zoneCode,
        state: window.locationData.state,
        district: window.locationData.district,
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

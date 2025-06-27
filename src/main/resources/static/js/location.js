const searchAddress = () => {
    let locationData = {};

    new daum.Postcode({
        oncomplete: (data) => {
            console.log('data=', data);
            // Kakao API로부터 받은 주소 데이터를 각각의 입력 필드에 할당
            document.getElementById("zonecode").value = data.zonecode;
            document.getElementById("address").value = data.roadAddress;
            document.getElementById("addressDetail").value = '';

            // window.locationData 객체에 값 저장
            // window.locationData = { state: data.sido, district: data.sigungu, zonecode: data.zonecode, address: data.roadAddress };

            locationData = {
                zonecode: data.zonecode,
                address: data.roadAddress,
                sido: data.sido,
                sigungu: data.sigungu,
            };

            registerLocation(locationData);
        }
    }).open();
}

const registerLocation = async (locationData) => {
    // const zoneCode = document.getElementById("zoneCode").value;
    // const address = document.getElementById("address").value;
    console.log('locationData=', locationData);
    const requestData = {
        // name: "등록된 주소",
        // zoneCode: zoneCode,
        // state: window.locationData.state,
        // district: window.locationData.district,
        // address: address,const searchAddress = () => {
        //     let locationData = {};
        //
        //     new daum.Postcode({
        //         oncomplete: (data) => {
        //             console.log('data=', data);
        //             // Kakao API로부터 받은 주소 데이터를 각각의 입력 필드에 할당
        //             document.getElementById("zonecode").value = data.zonecode;
        //             document.getElementById("address").value = data.roadAddress;
        //             document.getElementById("addressDetail").value = '';
        //
        //             // window.locationData 객체에 값 저장
        //             // window.locationData = { state: data.sido, district: data.sigungu, zonecode: data.zonecode, address: data.roadAddress };
        //
        //             locationData = {
        //                 zonecode: data.zonecode,
        //                 address: data.roadAddress,
        //                 sido: data.sido,
        //                 sigungu: data.sigungu,
        //             };
        //
        //             registerLocation(locationData);
        //         }
        //     }).open();
        // }
        //
        // const registerLocation = async (locationData) => {
        //     // const zoneCode = document.getElementById("zoneCode").value;
        //     // const address = document.getElementById("address").value;
        //     console.log('locationData=', locationData);
        //     const requestData = {
        //         // name: "등록된 주소",
        //         // zoneCode: zoneCode,
        //         // state: window.locationData.state,
        //         // district: window.locationData.district,
        //         // address: address,
        //         zonecode: locationData.zonecode,
        //         address: locationData.address,
        //         addressDetail: document.getElementById("addressDetail").value,
        //         sido: locationData.sido,
        //         sigungu: locationData.sigungu,
        //     };
        zonecode: locationData.zonecode,
        address: locationData.address,
        addressDetail: document.getElementById("addressDetail").value,
        sido: locationData.sido,
        sigungu: locationData.sigungu,
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

let locationData = null;

export function searchAddress() {
    new daum.Postcode({
        oncomplete: (data) => {
            document.getElementById("zonecode").value = data.zonecode;
            document.getElementById("address").value = data.roadAddress;
            document.getElementById("addressDetail").value = '';

            locationData = {
                zonecode: data.zonecode,
                address: data.roadAddress,
                sido: data.sido,
                sigungu: data.sigungu,
            };
        }
    }).open();
}

export function getLocationData() {
    if (!locationData) {
        const zonecode = document.getElementById('zonecode')?.value;
        const sido = document.getElementById('sido')?.value; // sido 필드가 있다면
        const sigungu = document.getElementById('sigungu')?.value; // sigungu 필드가 있다면
        const address = document.getElementById('address')?.value;
        const addressDetail = document.getElementById('addressDetail')?.value;

        if (zonecode && address) {
            locationData = {
                zonecode,
                sido,
                sigungu,
                address,
            };
        }
    }

    if (!locationData) {
        return null;
    }

    return {
        ...locationData,
        addressDetail: document.getElementById('addressDetail').value,
    };
}

document.addEventListener('DOMContentLoaded', () => {
    const zonecode = document.getElementById('zonecode')?.value;
    const sido = document.getElementById('sido')?.value;
    const sigungu = document.getElementById('sigungu')?.value;
    const address = document.getElementById('address')?.value;
    const addressDetail = document.getElementById('addressDetail')?.value;

    if (zonecode && address) {
        locationData = {
            zonecode,
            sido,
            sigungu,
            address,
        };
    }

    document.getElementById('searchAddress')
        .addEventListener('click', searchAddress);
});
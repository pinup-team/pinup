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
        return null;
    }

    return {
        ...locationData,
        addressDetail: document.getElementById('addressDetail').value,
    };
}

document.addEventListener('DOMContentLoaded', () => {
    document.getElementById('searchAddress')
        .addEventListener('click', searchAddress);
});
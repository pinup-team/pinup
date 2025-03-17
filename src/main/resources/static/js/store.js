document.addEventListener("DOMContentLoaded", function () {
    const carouselList = document.querySelector(".carousel-list");
    const items = document.querySelectorAll(".carousel-item");
    const prevButton = document.querySelector(".slide-left");
    const nextButton = document.querySelector(".slide-right");

    let currentIndex = 0;
    const totalItems = items.length;

    const firstClone = items[0].cloneNode(true);
    const lastClone = items[totalItems - 1].cloneNode(true);

    carouselList.appendChild(firstClone);
    carouselList.insertBefore(lastClone, items[0]);

    let realTotalItems = totalItems + 2;
    carouselList.style.transform = `translateX(-100%)`;

    function updateSlider() {
        carouselList.style.transition = "transform 0.5s ease-in-out";
        carouselList.style.transform = `translateX(-${(currentIndex + 1) * 100}%)`;
    }

    nextButton.addEventListener("click", function () {
        if (currentIndex >= totalItems) {
            setTimeout(() => {
                carouselList.style.transition = "none";
                carouselList.style.transform = `translateX(-100%)`;
                currentIndex = 0;
            }, 500);
        }
        currentIndex++;
        updateSlider();
    });

    prevButton.addEventListener("click", function () {
        if (currentIndex <= -1) {
            setTimeout(() => {
                carouselList.style.transition = "none";
                carouselList.style.transform = `translateX(-${totalItems * 100}%)`;
                currentIndex = totalItems - 1;
            }, 500);
        }
        currentIndex--;
        updateSlider();
    });

    setInterval(() => {
        nextButton.click();
    }, 3000);
});

function changeTab(tab) {
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
        const storeId = document.getElementById("storeId").value;
        console.log("게시판 리스트 로딩 시작: " + storeId);

        const postContainer = document.getElementById("post-list-container");
        postContainer.style.display = "block";

        fetch(`/post/${storeId}`)
            .then(response => response.text())
            .then(data => {
                postContainer.innerHTML = data;
            })
            .catch(error => console.error("게시판 리스트 로딩 중 오류:", error));
    }
}


/*function changeTab(tab) {
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    document.getElementById(`tab-${tab}`).classList.add('active');

    document.querySelectorAll('.tab-item').forEach(item => {
        item.classList.remove('active');
    });
    document.querySelector(`.tab-item[data-tab="${tab}"]`).classList.add('active');
}*/

/*
function changeTab(tab, storeId) {
    let newUrl = '';

    if (tab === 'info') {
        console.log('정보 탭 클릭');
        newUrl = `/stores/${storeId}`;  // 원하는 URL 형식으로 설정
        history.pushState(null, '', newUrl);
        document.getElementById('slide-bottom').style.display = 'block';
        document.getElementById('post-list-container').style.display = 'none';
    } else {
        console.log('게시판 리스트 로딩 시작'+storeId);
        newUrl = `/${tab}/${storeId}`;
        history.pushState(null, '', newUrl);
        document.getElementById('slide-bottom').style.display = 'none';
        document.getElementById('post-list-container').style.display = 'block';

        fetch(`/${tab}/${storeId}`)
            .then(response => response.text())
            .then(data => {
                document.getElementById('post-list-container').innerHTML = data;
            })
            .catch(error => console.error('게시판 리스트 로딩 중 오류:', error));
    }
}
*/

async function submitStore() {
    try {
        const locationId = await registerLocation();  // 주소 등록 후 ID 받기
        console.log("locationId", locationId);

        if (!locationId) {
            alert("주소 등록에 실패했습니다. 다시 시도해주세요.");
            return;
        }

        let locationInput = document.querySelector("input[name='locationId']");
        if (!locationInput) {
            locationInput = document.createElement("input");
            locationInput.type = "hidden";
            locationInput.name = "locationId";
            document.getElementById("storeForm").appendChild(locationInput);
        }
        locationInput.value = locationId;

        const form = document.getElementById("storeForm");
        const formData = new FormData(form);
        console.log("formData", formData);

        const formDataEntries = [];
        for (const pair of formData.entries()) {
            formDataEntries.push({ "Key": pair[0], "Value": pair[1] });
        }

        console.table(formDataEntries);

        const response = await fetch("/api/stores", {
            method: "POST",
            body: formData,
        })

        if (!response.ok) {
            alert("스토어 생성 api 오류");
            console.error(response.statusText);
        }

        const data = await response.json();
        console.log("data", data);

        if (data.id) {
            alert("스토어가 성공적으로 생성되었습니다.");
            window.location.href = `/stores/${data.id}`;
        } else {
            alert("스토어 생성에 실패했습니다.");
        }


    } catch (error) {
        console.error("주소 등록 및 게시물 생성 중 오류 발생:", error);
        alert("주소 등록 및 게시물 생성 중 오류가 발생했습니다.");
    }
}

// 탭 변경
/*
async function changeTab(tab) {
    try {
        console.log(`${tab} 탭 선택`);

        const storeId = document.getElementById("storeId").value;

        const newUrl = `/stores/${storeId}/${tab}`;
        history.pushState(null, '', newUrl);

        document.querySelectorAll(".tab-item").forEach(tabElement => {tabElement.classList.remove("active")});
        document.querySelector(`[data-tab="${tab}"]`).classList.add("active");

        document.querySelectorAll(".tab-content").forEach(content => content.style.display = "none");

        if (tab === "info") {
            document.getElementById("tab-content-area").style.display = "block";
            return;
        }

        if (tab === "media") {
            const mediaContent = document.getElementById("tab-content-area");
            mediaContent.innerHTML = "<p style='text-align:center; font-size:16px; color:gray;'>개발중인 기능입니다! 조금만 기다려 주세용 😘</p>";
            mediaContent.style.display = "block";
            return;
        }

        const contentDiv = document.getElementById(`tab-content-area`);

        if (!contentDiv.innerHTML.trim()) {
            const response = await fetch(`/stores/${storeId}/${tab}`);
            if (!response.ok) {
                throw new Error(`HTTP 오류 발생 (${response.status})`);
            }

            const html = await response.text();
            contentDiv.innerHTML = html;
            contentDiv.style.display = "block";
        } else {
            contentDiv.style.display = "block";
        }
    } catch (error) {
        console.error(`${tab} 탭 로딩 오류:`, error);
        alert(`"${tab}" 탭을 불러오는 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.`);
    }

}

window.addEventListener("popstate", function (event) {
    if (event.state && event.state.tab) {
        changeTab(event.state.tab);
    }
});
*/

document.addEventListener("DOMContentLoaded", function () {
    const createForm = document.getElementById("storeForm");
    const updateForm = document.getElementById("updateForm");
    const deleteButtons = document.querySelectorAll(".delete-btn");
    const imageInput = document.getElementById("imageFiles");
    const previewContainer = document.getElementById("previewContainer");

    if (imageInput) {
        imageInput.addEventListener("change", function (event) {
            const files = event.target.files;

            previewContainer.innerHTML = '';

            Array.from(files).forEach(file => {
                const reader = new FileReader();
                reader.onload = function (e) {
                    const previewImage = document.createElement("img");
                    previewImage.src = e.target.result;
                    previewImage.style.display = "block";
                    previewImage.style.margin = "10px";
                    previewContainer.appendChild(previewImage);
                };
                reader.readAsDataURL(file);
            });
        });
    }

    if (createForm) {
        createForm.addEventListener("submit", function (event) {
            event.preventDefault();

            console.log("createForm: ", createForm);

            const formData = new FormData(createForm);

            const jsonData = JSON.stringify({
                name: createForm.querySelector("input[name='name']").value,
                description: createForm.querySelector("textarea[name='description']").value,
                startDateTime: createForm.querySelector("input[name='startDate']").value,
                endDateTime: createForm.querySelector("input[name='endDate']").value,
                categoryId: createForm.querySelector("select[name='categoryId']").value,
                locationId: createForm.querySelector("input[name='locationId']").value
            })

            formData.append("request", new Blob([jsonData], {type: "application/json"}));

            // for (const key in jsonData) {
            //     formData.append(key, jsonData[key]);
            // }

            const imageFiles = createForm.querySelector("input[name='imageFiles']").files;
            for (let i = 0; i < imageFiles.length; i++) {
                formData.append("imageFiles", imageFiles[i]);
            }

            console.log("formData: ", formData);

            fetch("/api/stores", {
                method: "POST",
                body: formData,
            })
                .then(response => response.json())
                .then(data => {
                    console.log("서버 응답:", data);
                    alert("팝업스토어 등록 성공");
                    window.location.href = `/stores/${data.id}`;
                })
                .catch(error => {
                    console.error("팝업스토어 등록 실패:", error);
                    alert("팝업스토어 등록 중 오류 발생");
                });
        });
    }


    if (updateForm) {
        updateForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const storeId = updateForm.dataset.storeId;
            const formData = new FormData(updateForm);

            const jsonData = {
                name: updateForm.querySelector("input[name='name']").value,
                description: updateForm.querySelector("input[name='description']").value,
                startDateTime: updateForm.querySelector("input[name='startDate']").value,
                endDateTime: updateForm.querySelector("input[name='endDate']").value,
                categoryId: updateForm.querySelector("select[name='categoryId']").value,
                locationId: updateForm.querySelector("input[name='locationId']").value,
            };

            formData.append("storeRequest", new Blob([JSON.stringify(jsonData)], {type: "application/json"}));

            const imageFiles = updateForm.querySelector("input[name='imageFiles']").files;
            for (let i = 0; i < imageFiles.length; i++) {
                formData.append("imageFiles", imageFiles[i]);
            }

            fetch(`/api/stores/${storeId}`, {
                method: "PATCH",
                body: formData
            })
                .then(response => response.json())
                .then(() => {
                    alert("팝업스토어 수정 완료");
                    window.location.href = `/stores/${storeId}`;
                })
                .catch(error => {
                    console.error("팝업스토어 수정 실패:", error);
                    alert("팝업스토어 수정 중 오류 발생");
                });
        });
    }

    deleteButtons.forEach(button => {
        button.addEventListener("click", function () {
            const storeId = this.dataset.storeId;
            if (!confirm("스토어를 삭제하겠습니까?")) return;

            fetch(`/api/stores/${storeId}`, {
                method: "DELETE"
            })
                .then(() => {
                    alert("팝업스토어 삭제 완료");
                    window.location.href = "/stores";
                })
                .catch(error => {
                    console.error("팝업스토어 삭제 실패:", error);
                    alert("팝업스토어 삭제 중 오류 발생");
                });
        });
    });
});



document.addEventListener("DOMContentLoaded", function () {
    const createForm = document.getElementById("storeForm");
    const updateForm = document.getElementById("updateForm");
    const deleteButtons = document.querySelectorAll(".delete-btn");

    if (createForm) {
        createForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const formData = new FormData(createForm);
            const jsonData = {};
            formData.forEach((value, key) => {
                jsonData[key] = value;
            });

            console.log(formData);
            console.log(jsonData);

            fetch("/api/stores", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(jsonData)
            })
                .then(response => response.json())
                .then(data => {
                    alert("팝업스토어 등록 성공");
                    window.location.href = `/stores/${data.id}`;
                })
                .catch(error => {
                    console.error("팝업스토어 등록 실패:", error);
                    alert("팝업스토어 등록 중 오류 발생");
                });
        });
    }


    if (updateForm) {
        updateForm.addEventListener("submit", function (event) {
            event.preventDefault();

            const storeId = updateForm.dataset.storeId;
            const formData = new FormData(updateForm);
            const jsonData = {};
            formData.forEach((value, key) => {
                jsonData[key] = value;
            });

            fetch(`/api/stores/${storeId}`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(jsonData)
            })
                .then(response => response.json())
                .then(() => {
                    alert("팝업스토어 수정 완료");
                    window.location.href = `/stores/${storeId}`;
                })
                .catch(error => {
                    console.error("팝업스토어 수정 실패:", error);
                    alert("팝업스토어 수정 중 오류 발생");
                });
        });
    }

    deleteButtons.forEach(button => {
        button.addEventListener("click", function () {
            const storeId = this.dataset.storeId;
            if (!confirm("스토어를 삭제하겠습니까?")) return;

            fetch(`/api/stores/${storeId}`, {
                method: "DELETE"
            })
                .then(() => {
                    alert("팝업스토어 삭제 완료");
                    window.location.href = "/stores";
                })
                .catch(error => {
                    console.error("팝업스토어 삭제 실패:", error);
                    alert("팝업스토어 삭제 중 오류 발생");
                });
        });
    });
});

// 카카오 지도 API 로드 함수
// 카카오 지도 API 로드 함수
function loadMap() {
    var latitude = document.getElementById("latitude-hidden").value;
    var longitude = document.getElementById("longitude-hidden").value;
    var storeName = document.getElementById("store-name") ? document.getElementById("store-name").textContent.trim() : "매장 위치";
    var storeAddr = document.getElementById("store-address") ? document.getElementById("store-address").textContent.trim() : "";

    console.log("Store Name: ", storeName);
    console.log("Store Address: ", storeAddr);

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
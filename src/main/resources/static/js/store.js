 document.addEventListener("DOMContentLoaded", function () {
    const carouselList = document.querySelector(".carousel-list");
    const items = document.querySelectorAll(".carousel-item");
    const prevButton = document.querySelector(".slide-left");
    const nextButton = document.querySelector(".slide-right");

    const itemWidth = 500;
    let currentIndex = 0;
    const maxIndex = items.length - 2;

    function updateSlider() {
        const offset = currentIndex * itemWidth;
        carouselList.style.transform = `translateX(-${offset}px)`;
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


async function submitStore() {
    try {
        const locationId = await registerLocation();  // 주소 등록 후 ID 받기

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

        const days = document.getElementsByName("days");
        const startTimes = document.getElementsByName("startTimes");
        const endTimes = document.getElementsByName("endTimes");

        for (let i = 0; i < days.length; i++) {
            formData.append(`operatingHours[${i}].day`, days[i].value);
            formData.append(`operatingHours[${i}].startTime`, startTimes[i].value);
            formData.append(`operatingHours[${i}].endTime`, endTimes[i].value);
        }

        const snsUrl = document.querySelector("input[name='snsUrl']").value;
        const websiteUrl = document.querySelector("input[name='websiteUrl']").value;
        const contactNumber = document.querySelector("input[name='contactNumber']").value;

        if (snsUrl) formData.set("snsUrl", snsUrl);
        if (websiteUrl) formData.set("websiteUrl", websiteUrl);
        if (contactNumber) formData.set("contactNumber", contactNumber);

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
            return;
        }

        const data = await response.json();

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

            Array.from(files).forEach((file, index) => {
                const reader = new FileReader();
                reader.onload = function (e) {
                    const previewWrapper = document.createElement("div");
                    previewWrapper.style.margin = "10px";
                    previewWrapper.style.textAlign = "center";

                    const previewImage = document.createElement("img");
                    previewImage.src = e.target.result;
                    previewImage.style.width = "150px";
                    previewImage.style.cursor = "pointer";
                    previewImage.dataset.index = index;

                    const radio = document.createElement("input");
                    radio.type = "radio";
                    radio.name = "thumbnail";
                    radio.value = index;
                    radio.style.marginTop = "5px";

                    if (index === 0) {
                        radio.checked = true;
                        document.getElementById("thumbnailImage").value = 0;
                        previewImage.classList.add("selected-thumbnail");
                    }

                    radio.addEventListener("change", () => {
                        document.getElementById("thumbnailImage").value = index;

                        document.querySelectorAll("#previewContainer img").forEach(img => {
                            img.classList.remove("selected-thumbnail");
                        });

                        previewImage.classList.add("selected-thumbnail");
                    });

                    previewImage.addEventListener("click", () => {
                        radio.checked = true;
                        radio.dispatchEvent(new Event("change"));
                    });

                    previewWrapper.appendChild(previewImage);
                    previewWrapper.appendChild(document.createElement("br"));
                    previewWrapper.appendChild(radio);
                    previewContainer.appendChild(previewWrapper);
                };
                reader.readAsDataURL(file);
            });
        });
    }


    if (createForm) {
        createForm.addEventListener("submit", function (event) {
            event.preventDefault();

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

            fetch("/api/stores", {
                method: "POST",
                body: formData,
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

document.addEventListener("DOMContentLoaded", function () {
    const addButton = document.getElementById("addOperatingHour");
    if (addButton) {
        addButton.addEventListener("click", addOperatingHour);
    }
});

function addOperatingHour() {
    const container = document.getElementById("operatingHoursContainer");
    const div = document.createElement("div");
    div.className = "operating-hour";
    div.innerHTML = `
        <input type="text" name="days" placeholder="요일 (예: 월, 월~목)" required />
        <input type="time" name="startTimes" required />
        <input type="time" name="endTimes" required />
        <button type="button" onclick="removeOperatingHour(this)">-</button>
    `;
    container.appendChild(div);
}

function removeOperatingHour(button) {
    button.parentElement.remove();
}
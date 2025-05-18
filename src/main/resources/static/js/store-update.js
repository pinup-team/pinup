document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("updateForm");
    const imageInput = document.getElementById("imageFiles");
    const previewContainer = document.getElementById("previewContainer");
    const addButton = document.getElementById("addOperatingHour");
    const operatingHoursContainer = document.getElementById("operatingHoursContainer");

    // 운영 시간 추가 버튼
    if (addButton) {
        addButton.addEventListener("click", addOperatingHour);
    }

    function addOperatingHour() {
        if (!operatingHoursContainer) return;

        const div = document.createElement("div");
        div.className = "operating-hour";
        div.innerHTML = `
            <input type="text" name="days" placeholder="요일 (예: 월, 월~목)" required />
            <input type="time" name="startTimes" required />
            <input type="time" name="endTimes" required />
            <button type="button" class="remove-btn">-</button>
        `;
        operatingHoursContainer.appendChild(div);
    }

    // 이벤트 위임으로 삭제 버튼 처리
    operatingHoursContainer.addEventListener("click", (event) => {
        if (event.target.classList.contains("remove-btn")) {
            event.target.parentElement.remove();
        }
    });

    // 이미지 미리보기
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

    window.submitUpdateForm = async function () {
        try {
            const formData = new FormData(form);

            const days = document.getElementsByName("days");
            const startTimes = document.getElementsByName("startTimes");
            const endTimes = document.getElementsByName("endTimes");

            for (let i = 0; i < days.length; i++) {
                formData.append(`operatingHours[${i}].day`, days[i].value);
                formData.append(`operatingHours[${i}].startTime`, startTimes[i].value);
                formData.append(`operatingHours[${i}].endTime`, endTimes[i].value);
            }

            const response = await fetch(`/api/stores/${form.dataset.storeId}`, {
                method: "PATCH",
                body: formData,
            });

            if (!response.ok) {
                alert("스토어 수정 API 오류");
                console.error(await response.text());
                return;
            }

            const data = await response.json();

            if (data.id) {
                alert("스토어가 성공적으로 수정되었습니다.");
                window.location.href = `/stores/${data.id}`;
            } else {
                alert("스토어 수정에 실패했습니다.");
            }

        } catch (error) {
            console.error("스토어 수정 중 오류 발생:", error);
            alert("스토어 수정 중 오류가 발생했습니다.");
        }
    }
});

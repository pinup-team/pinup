document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("storeForm");
    const imageInput = document.getElementById("imageFiles");
    const previewContainer = document.getElementById("previewContainer");
    const addButton = document.getElementById("addOperatingHour");

    if (addButton) {
        addButton.addEventListener("click", addOperatingHour);
    }

    window.addOperatingHour = function () {
        const container = document.getElementById("operatingHoursContainer");
        if (!container) return;

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

    window.removeOperatingHour = function (button) {
        button.parentElement.remove();
    }

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

    window.submitStore = async function () {
        try {
            const locationId = await registerLocation();

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

            console.table(Array.from(formData.entries()));

            const response = await fetch("/api/stores", {
                method: "POST",
                body: formData,
            });

            if (!response.ok) {
                alert("스토어 생성 api 오류");
                console.error(await response.text());
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

});

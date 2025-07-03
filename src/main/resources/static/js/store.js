import { getLocationData } from "./locationData.js";
import { registerLocation, updateLocation } from "./locationApi.js";
import ImagePreview from "./storeImagePreview.js";

function addOperatingHour() {
    const container = document.getElementById("operatingHoursContainer");
    if (!container) return;

    const div = document.createElement("div");
    div.className = "operating-hour";
    div.innerHTML = `
            <input type="text" name="days" placeholder="요일 (예: 월, 월~목)" required />
            <input type="time" name="startTimes" required />
            <input type="time" name="endTimes" required />
            <button type="button" class="addOperatingHour">+</button>
            <button type="button" class="removeOperatingHour">-</button>
        `;
    container.appendChild(div);
}

function removeOperatingHour(button) {
    const operatingHourElement = button.closest('.operating-hour');
    if (operatingHourElement) {
        operatingHourElement.remove();
    }
}

function buildFormData(form) {
    const formData = new FormData(form);

    const days = document.getElementsByName('days');
    const startTimes = document.getElementsByName('startTimes');
    const endTimes = document.getElementsByName('endTimes');

    for (let i = 0; i < days.length; i++) {
        formData.append(`operatingHours[${i}].day`, days[i].value);
        formData.append(`operatingHours[${i}].startTime`, startTimes[i].value);
        formData.append(`operatingHours[${i}].endTime`, endTimes[i].value);
    }

    const snsUrl = document.getElementById('snsUrl').value;
    const websiteUrl = document.getElementById('websiteUrl').value;

    if (snsUrl) formData.set('snsUrl', snsUrl);
    if (websiteUrl) formData.set('websiteUrl', websiteUrl);

    return formData;
}

async function handleSubmit(action) {
    const locationData = getLocationData();
    console.log('locationData=', locationData);
    if (!locationData) {
        alert('주소 검색을 먼저 해주세요.');
        return;
    }

    try {
        if (action === 'create') {
            const location = await registerLocation(locationData);
            document.getElementById('locationId').value = location.id;

            const form = document.getElementById('storeForm');
            const formData = buildFormData(form);

            const response = await fetch('/api/stores', {
                method: 'POST',
                body: formData,
            });

            if (!response.ok) {
                alert("스토어 생성 api 오류");
                console.error(await response.text());
                return;
            }

            const data = await response.json();

            alert("스토어가 성공적으로 생성되었습니다.");
            window.location.href = `/stores/${data.id}`;
        } else if (action === 'update') {
            const locationId = document.getElementById('locationId').value;
            await updateLocation(locationId, locationData);

            const form = document.getElementById('updateForm');
            const formData = new FormData();

            const storeRequest = {
                name: form.name.value,
                description: form.description.value,
                startDate: form.startDate.value,
                endDate: form.endDate.value,
                categoryId: form.categoryId.value,
                websiteUrl: form.websiteUrl ? form.websiteUrl.value : "",
                snsUrl: form.snsUrl ? form.snsUrl.value : "",
                thumbnailImage: form.thumbnailImage.value || 0,
                operatingHours: Array.from(document.querySelectorAll(".operating-hour")).map(hour => ({
                    day: hour.querySelector("input[name='days']").value,
                    startTime: hour.querySelector("input[name='startTimes']").value,
                    endTime: hour.querySelector("input[name='endTimes']").value,
                }))
            };

            formData.append("request", new Blob([JSON.stringify(storeRequest)], { type: "application/json" }));

            // 🖼️ 이미지 파일 추가
            const imageFiles = document.getElementById("imageFiles").files;
            for (const file of imageFiles) {
                formData.append("imageFiles", file);
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
        }
    } catch (error) {
        console.error("주소 등록 및 게시물 생성 중 오류 발생:", error);
        alert("주소 등록 및 게시물 생성 중 오류가 발생했습니다.");
    }
}

document.addEventListener('DOMContentLoaded', () => {
    const operatingHourContainer = document.getElementById('operatingHoursContainer');
    if (operatingHourContainer) {
        operatingHourContainer.addEventListener('click', (event) => {
            const operatingHours = operatingHourContainer.querySelectorAll('.operating-hour');
            const classList = event.target.classList;
            if (classList.contains('removeOperatingHour')) {
                removeOperatingHour(event.target);
            }
            if (classList.contains('addOperatingHour')) {
                if (operatingHours.length > 4) {
                    alert('운영 시간 작성은 최대 5개까지 입니다.');
                    return;
                }
                addOperatingHour();
            }
        });
    }

    ImagePreview.init();

    const createSubmitButton = document.getElementById('createSubmitButton');
    const updateSubmitButton = document.getElementById('updateSubmitButton');
    if (createSubmitButton) {
        createSubmitButton.addEventListener('click', () => handleSubmit('create'));
    }
    if (updateSubmitButton) {
        updateSubmitButton.addEventListener('click', () => handleSubmit('update'));
    }
});

window.addEventListener('beforeunload', () => {
    ImagePreview.cleanup();
});
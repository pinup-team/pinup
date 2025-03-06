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

            const formData = new FormData(createForm);

            const jsonData = JSON.stringify({
                name: createForm.querySelector("input[name='name']").value,
                description: createForm.querySelector("input[name='description']").value,
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

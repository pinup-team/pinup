document.addEventListener("DOMContentLoaded", function () {
    const form = document.getElementById("updateForm");
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

    if (form) {
        form.addEventListener("submit", function (event) {
            event.preventDefault();
            const storeId = form.dataset.storeId;
            updateStore(storeId);
        });
    }
});
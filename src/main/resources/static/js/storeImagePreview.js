const ImagePreview = (() => {
    let selectedThumbnailId = '';
    let selectedThumbnailIndex = '';
    let existingImagesCount = 0;

    let previewContainer;
    let newPreviewContainer;
    let thumbnailIdElement;
    let thumbnailIndexElement;

    function init() {
        const imageElement = document.getElementById('images');
        const newImageElement = document.getElementById('updateImages');

        previewContainer = document.getElementById('previewContainer');
        newPreviewContainer = document.getElementById('newPreviewContainer');

        thumbnailIdElement = document.getElementById('thumbnailId');
        thumbnailIndexElement = document.getElementById('thumbnailIndex');

        existingImagesCount = previewContainer ? previewContainer.querySelectorAll('.image-wrapper').length : 0;

        if (previewContainer) {
            previewContainer.querySelectorAll('.image-preview').forEach(img => {
                img.addEventListener('click', () => setThumbnail(previewContainer, 'existing', img.dataset.id));
            });
        }

        if (imageElement) {
            imageElement.addEventListener('change', handleImageChange);
        }
        if (newImageElement) {
            newImageElement.addEventListener('change', handleNewImageChange);
        }

        if (thumbnailIdElement?.value) {
            selectedThumbnailId = thumbnailIdElement.value;
            updateThumbnailUI(previewContainer, 'existing', thumbnailIdElement.value);
        }
    }

    function handleImageChange(event) {
        const files = Array.from(event.target.files);

        cleanup(previewContainer);
        previewContainer.innerHTML = '';

        if (files.length === 0) return;

        files.forEach((file, index) => createImagePreview(file, previewContainer, 'new', index));

        if (files.length > 0) {
            setThumbnail(previewContainer, 'new', 0);
        }
    }

    function handleNewImageChange(event) {
        const files = Array.from(event.target.files);

        cleanup(newPreviewContainer);
        newPreviewContainer.innerHTML = '';

        if (files.length === 0) return;

        files.forEach((file, index) => createImagePreview(file, newPreviewContainer, 'new', index));
    }

    async function createImagePreview(file, container, type, index, id) {
        const imageUrl = URL.createObjectURL(file);
        const wrapper = createPreviewElement(imageUrl, container, type, index, id);
        container.appendChild(wrapper);
    }

    function createPreviewElement(imageUrl, container, type, index, id) {
        const wrapper = document.createElement('div');
        wrapper.className = 'image-wrapper';
        wrapper.style.cssText = 'margin: 10px; text-align: center; display: inline-block;';
        wrapper.dataset.type = type;
        wrapper.dataset.id = id;

        const img = document.createElement('img');
        img.src = imageUrl;
        img.classList.add("image-preview");
        img.style.cssText = `
            width: 150px;
            cursor: pointer;
            border-radius: 8px;
        `;
        if (type === 'existing') {
            img.dataset.id = id;
        } else {
            img.dataset.index = index;
        }
        img.addEventListener('click', () => setThumbnail(container, type, type === 'existing' ? id : index));

        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'thumbnail';
        radio.value = type === 'existing' ? id : index;
        radio.style.marginTop = '5px';
        radio.addEventListener('change', () => setThumbnail(container, type, type === 'existing' ? id : index, type));

        wrapper.appendChild(img);
        wrapper.appendChild(document.createElement('br'));
        wrapper.appendChild(radio);

        return wrapper;
    }

    function setThumbnail(container, type, value) {
        if (type === 'existing') {
            selectedThumbnailId = value;
            selectedThumbnailIndex = '';
            thumbnailIdElement.value = value;
            thumbnailIndexElement.value = '';
        } else {
            selectedThumbnailId = '';
            selectedThumbnailIndex = value;
            thumbnailIdElement.value = '';
            thumbnailIndexElement.value = value;
        }
        updateThumbnailUI(container, type, value);
    }

    function updateThumbnailUI(container, type, value) {
        [previewContainer, newPreviewContainer].forEach(contain => {
            if (contain) {
                contain.querySelectorAll('img').forEach(img => {
                    img.classList.remove('selected-thumbnail');
                });
                contain.querySelectorAll('input[type="radio"]').forEach(radio => {
                    radio.checked = false;
                });
            }
        });

        let selectedImg, selectedRadio;
        if (type === 'existing') {
            selectedImg = container?.querySelector(`img[data-id="${value}"]`);
            selectedRadio = container?.querySelector(`input[value="${value}"]`);
        } else {
            selectedImg = container?.querySelector(`img[data-index="${value}"]`);
            selectedRadio = container?.querySelector(`input[value="${value}"]`);
        }

        if (selectedImg) selectedImg.classList.add('selected-thumbnail');
        if (selectedRadio) selectedRadio.checked = true;
    }

    function cleanup(container) {
        if (container) {
            container.querySelectorAll('img').forEach(img => {
                if (img.src.startsWith('blob:')) {
                    URL.revokeObjectURL(img.src);
                }
            });
        }
    }

    return {
        init,
        getThumbnailIndex: () => selectedIndex,
        cleanup: () => {
            cleanup(previewContainer);
            cleanup(newPreviewContainer);
        }
    };
})();

export default ImagePreview;
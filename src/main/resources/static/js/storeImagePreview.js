const ImagePreview = (() => {
    let selectedIndex = 0;
    let previewContainer;
    let thumbnailElement;

    function init() {
        const imageElement = document.getElementById('images');
        previewContainer = document.getElementById('previewContainer');
        thumbnailElement = document.getElementById('thumbnailIndex');

        if (imageElement) {
            imageElement.addEventListener('change', handleImageChange);
        }
    }

    function handleImageChange(event) {
        const files = Array.from(event.target.files);

        cleanup();
        previewContainer.innerHTML = '';

        if (files.length === 0) return;

        files.forEach((file, index) => createImagePreview(file, index));
        setThumbnail(0);
    }

    async function createImagePreview(file, index) {
        const imageUrl = URL.createObjectURL(file);
        const wrapper = createPreviewElement(imageUrl, index);
        previewContainer.appendChild(wrapper);
    }

    function cleanup() {
        previewContainer.querySelectorAll('img').forEach(img => {
            if (img.src.startsWith('blob:')) {
                URL.revokeObjectURL(img.src);
            }
        })
    }

    function createPreviewElement(imageUrl, index) {
        const wrapper = document.createElement('div');
        wrapper.style.cssText = 'margin: 10px; text-align: center; display: inline-block;';

        const img = document.createElement('img');
        img.src = imageUrl;
        img.dataset.index = index;
        img.classList.add("selected-thumbnail");
        img.style.cssText = `
            width: 150px;
            cursor: pointer;
            border-radius: 8px;
        `;
        img.addEventListener('click', () => setThumbnail(index));

        const radio = document.createElement('input');
        radio.type = 'radio';
        radio.name = 'thumbnail';
        radio.value = index;
        radio.style.marginTop = '5px';
        radio.addEventListener('change', () => setThumbnail(index));

        wrapper.appendChild(img);
        wrapper.appendChild(document.createElement('br'));
        wrapper.appendChild(radio);

        return wrapper;
    }

    function setThumbnail(index) {
        selectedIndex = index;
        thumbnailElement.value = index;
        updateThumbnailUI(index);
    }

    function updateThumbnailUI(index) {
        previewContainer.querySelectorAll('img').forEach(img => {
            img.classList.remove('selected-thumbnail');
        });

        previewContainer.querySelectorAll('input[type="radio"]').forEach(radio => {
            radio.checked = false;
        });

        const selectedImg = previewContainer.querySelector(`img[data-index="${index}"]`);
        const selectedRadio = previewContainer.querySelector(`input[value="${index}"]`);

        if (selectedImg) selectedImg.classList.add('selected-thumbnail');
        if (selectedRadio) selectedRadio.checked = true;
    }

    return {
        init,
        getThumbnailIndex: () => selectedIndex,
        cleanup,
    };
})();

export default ImagePreview;
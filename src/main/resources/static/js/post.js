let isUpdateMode = false;

function validateForm() {
    const isValid = checkImageCount();
    if (!isValid) {
        alert("최종 이미지 수가 2장 이상이어야 합니다.");
        return false;
    }
    return true;
}
function checkImageCount() {
    const uploaded = document.getElementById("images").files.length;
    const fileCountMessage = document.getElementById("fileCountMessage");

    let total = uploaded;

    if (isUpdateMode) {
        const checkboxes = document.querySelectorAll('input[name="imagesToDelete"]');
        const checked = document.querySelectorAll('input[name="imagesToDelete"]:checked');
        const remaining = checkboxes.length - checked.length;

        total += remaining;

        console.log(`[이미지 검사] 모드: 수정 / 기존:${checkboxes.length}, 삭제:${checked.length}, 남음:${remaining} / 첨부:${uploaded} / 총:${total}`);

        if (total < 2) {
            fileCountMessage.innerText = "기존 이미지와 첨부한 이미지를 합쳐서 최소 2장은 있어야 합니다.";
            fileCountMessage.style.display = "inline";
            return false;
        }
    } else {
        console.log(`[이미지 검사] 모드: 생성 / 첨부:${uploaded}`);

        if (uploaded < 2) {
            fileCountMessage.innerText = "이미지는 최소 2장 이상 등록해야 합니다.";
            fileCountMessage.style.display = "inline";
            return false;
        }
    }

    fileCountMessage.style.display = "none";
    return true;
}

document.addEventListener('DOMContentLoaded', function () {
    const updatePostForm = document.getElementById('updatePostForm');
    if (updatePostForm) {
        isUpdateMode = true;
        updatePostForm.addEventListener('submit', function (event) {
            event.preventDefault();
            if (!validateForm()) return;

            const postId = document.getElementById('postId').value;
            const title = document.getElementById('title').value;
            const content = document.getElementById('content').value;
            const images = document.getElementById('images').files;
            const imagesToDelete = document.getElementById('imagesToDelete').value.split(',').filter(Boolean);

            const updatePostRequest = {
                title: title,
                content: content
            };

            const formData = new FormData();
            formData.append("updatePostRequest", new Blob([JSON.stringify(updatePostRequest)], { type: "application/json" }));

            for (let i = 0; i < images.length; i++) {
                formData.append("images", images[i]);
            }

            for (let i = 0; i < imagesToDelete.length; i++) {
                formData.append("imagesToDelete", imagesToDelete[i]);
            }

            fetch('/api/post/' + postId, {
                method: 'PUT',
                body: formData,
            })
                .then(response => response.json())
                .then(data => {
                    if (data.storeId) {
                        alert("게시물이 성공적으로 업데이트되었습니다!");
                        window.location.href = `/stores/${data.storeId}`;
                    } else {
                        alert("게시물 업데이트에 실패했습니다.");
                    }
                })
                .catch(error => {
                    console.error("게시물 업데이트 중 오류 발생:", error);
                    alert("게시물 업데이트 중에 오류가 발생했습니다.");
                });
        });
    }

    initializeCarousel();
});

document.addEventListener("DOMContentLoaded", function () {
    initializeCarousel();
});

function toggleImageToDelete(checkbox) {
    const imagesToDeleteField = document.getElementById("imagesToDelete");
    let imagesToDelete = imagesToDeleteField.value ? new Set(imagesToDeleteField.value.split(',')) : new Set();

    if (checkbox.checked) {
        imagesToDelete.add(checkbox.value);
    } else {
        imagesToDelete.delete(checkbox.value);
    }

    imagesToDeleteField.value = Array.from(imagesToDelete).join(',');

    // ✅ 실제 삭제가 발생할 때만 검사
    const checkedCount = document.querySelectorAll('input[name="imagesToDelete"]:checked').length;

    if (checkedCount > 0) {
        checkImageCount();
    }
}

function submitPost() {
    const form = document.getElementById("postForm");
    const images = document.getElementById("images").files;

    if (images.length < 2) {
        alert("이미지는 최소 2장 이상 등록해야 합니다.");
        return;
    }
    const formData = new FormData();
    const postData = {
        storeId: form.storeId.value,
        title: form.title.value,
        content: form.content.value
    };
    formData.append("post", new Blob([JSON.stringify(postData)], { type: "application/json" }));

    for (let i = 0; i < images.length; i++) {
        formData.append("images", images[i]);
    }

    fetch("/api/post/create", {
        method: "POST",
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.storeId) {
                alert("게시물이 성공적으로 생성되었습니다!");
                window.location.href = `/stores/${data.storeId}`;
            } else {
                alert("게시물 생성에 실패했습니다.");
            }
        })
        .catch(error => {
            console.error("게시물 생성 중 오류 발생:", error);
            alert("게시물을 생성하는 중에 오류가 발생했습니다.");
        });
}

function removePost(postId, storeId) {
    if (confirm("이 게시물을 삭제하시겠습니까?")) {
        fetch(`/api/post/${postId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    alert("게시물이 성공적으로 삭제되었습니다!");
                    window.location.href = `/stores/${storeId}`;
                } else {
                    alert("게시물 삭제에 실패했습니다.");
                }
            })
            .catch(error => {
                console.error("게시물 삭제 중 오류 발생:", error);
                alert("게시물 삭제 중에 오류가 발생했습니다.");
            });
    }
}

function disablePost(postId, storeId) {
    if (confirm("이 게시물을 삭제하시겠습니까?")) {
        fetch(`/api/post/${postId}/disable`, {
            method: 'PATCH'
        })
            .then(response => {
                if (response.ok) {
                    alert("게시물이 성공적으로 삭제되었습니다!");
                    window.location.href = `/stores/${storeId}`;
                } else {
                    alert("게시물 삭제에 실패했습니다.");
                }
            })
            .catch(error => {
                console.error("게시물 삭제 중 오류 발생:", error);
                alert("게시물 삭제 중에 오류가 발생했습니다.");
            });
    }
}

function fileCheck(event) {
    const fileInput = event.target;
    const fileName = document.getElementById("fileName");
    const previewContainer = document.getElementById("previewContainer");
    const fileCountMessage = document.getElementById("fileCountMessage");

    previewContainer.innerHTML = "";
    const files = fileInput.files;

    fileName.innerText = files.length > 0 ? `${files.length}개의 파일 선택됨` : "선택된 파일 없음";

    for (let i = 0; i < files.length; i++) {
        const file = files[i];
        if (!file.type.startsWith("image/")) {
            alert("이미지 파일만 업로드 가능합니다!");
            continue;
        }

        const reader = new FileReader();
        reader.onload = function(e) {
            const img = document.createElement("img");
            img.src = e.target.result;
            img.classList.add("preview-img");
            previewContainer.appendChild(img);
        };
        reader.readAsDataURL(file);
    }
    if (event.target.files.length > 0) {
        checkImageCount();
    }
}

function fileUpload(updateMode = false) {
    isUpdateMode = updateMode;
    document.getElementById("images").click();
}

function openDetailPopup(postId) {
    const modal = document.getElementById("detailModal");
    const detailContainer = document.getElementById("detailContainer");

    detailContainer.innerHTML = "로딩 중...";

    fetch(`/post/${postId}`)
        .then(response => response.text())
        .then(html => {
            detailContainer.innerHTML = html;
            // 🔥 모달 내부의 JavaScript 실행
            initializeCarousel();        // 🔥 특히 캐러셀 관련 바인딩
            initializeCommentButton();   // 💬 댓글 버튼 이벤트 바인딩
            initializeCommentHandlers(); // 💬 댓글 생성&삭제  바인딩
        })
        .catch(error => {
            detailContainer.innerHTML = "데이터를 불러올 수 없습니다.";
        });

    modal.style.display = "flex";

    modal.removeEventListener('click', popupClickHandler);
    modal.addEventListener('click', popupClickHandler);
}

function popupClickHandler(e) {
    const popupContent = document.querySelector(".post-content");

    if (!popupContent.contains(e.target)) {
        closeDetailPopup();
    }
}

function closeDetailPopup() {
    document.getElementById("detailModal").style.display = "none";
}

function initializeCarousel() {
    var carousels = document.getElementsByClassName("carousel");

    for (var i = 0; i < carousels.length; i++) {
        addEvenToCarousel(carousels[i]);
    }
}

function addEvenToCarousel(carouselElem) {
    var ulElem = carouselElem.querySelector('ul');
    var liElems = ulElem.querySelectorAll('li');

    var liWidth = liElems[0].clientWidth;
    var adjusteWidth = liElems.length * liWidth;
    ulElem.style.width = adjusteWidth + 'px';

    var slideButtons = carouselElem.querySelectorAll('.slide');
    for (var i = 0; i < slideButtons.length; i++) {
        slideButtons[i].addEventListener('click', createListenerSlide(carouselElem));
    }
}

function createListenerSlide(carouselElem) {
    return function (event) {
        var clickedButton = event.currentTarget;

        var liElems = carouselElem.querySelectorAll('li');
        var liCount = liElems.length;
        var currentIndex = carouselElem.attributes.data.value;

        if (clickedButton.className.includes('right') && currentIndex < liCount - 1) {
            currentIndex++;
            scrollDiv(carouselElem, currentIndex);
        } else if (clickedButton.className.includes('left') && currentIndex > 0) {
            currentIndex--;
            scrollDiv(carouselElem, currentIndex);
        }

        updateIndicator(carouselElem, currentIndex);

        updateSlideButtonVisible(carouselElem, currentIndex, liCount);

        carouselElem.attributes.data.value = currentIndex;
    }
}

function scrollDiv(carouselElem, nextIndex) {
    var ulElem = carouselElem.querySelector('ul');
    var liElems = ulElem.querySelectorAll('li');

    var liWidth = liElems[0].clientWidth;
    var newLeft = -liWidth * nextIndex;

    ulElem.style.transform = `translateX(${newLeft}px)`;
}

function updateIndicator(carouselElem, currentIndex) {
    var indicators = carouselElem.querySelectorAll('.pinup-carousel-indicators .pinup-indicator');

    for (var i = 0; i < indicators.length; i++) {
        if (currentIndex === i) {
            indicators[i].classList.add('active'); // 🔹 active 클래스 추가
        } else {
            indicators[i].classList.remove('active'); // 🔹 active 클래스 제거
        }
    }
}

function updateSlideButtonVisible(carouselElem, currentIndex, liCount) {
    var left = carouselElem.querySelector('.slide-left');
    var right = carouselElem.querySelector('.slide-right');

    if (currentIndex > 0) {
        left.style.display = 'block';
    } else {
        left.style.display = 'none';
    }

    if (currentIndex < liCount - 1) {
        right.style.display = 'block';
    } else {
        right.style.display = 'none';
    }
}

function initializeCommentButton() {
    const commentButton = document.querySelector(".pinup-comment-btn");
    const commentInput = document.querySelector(".pinup-comment-input");

    commentButton.removeEventListener("click", focusCommentInput);
    commentButton.addEventListener("click", focusCommentInput);
}

function focusCommentInput() {
    const commentInput = document.querySelector(".pinup-comment-input");
    if (commentInput) {
        commentInput.focus();
    }
}

function initializeCommentHandlers() {


    const commentForm = document.getElementById("comment-form");
    const commentList = document.querySelector(".card-content ul");

    if (commentList) {
        commentList.addEventListener("click", async (e) => {
            if (e.target.matches(".transparent-button[data-comment-id]")) {
                const commentId = e.target.getAttribute("data-comment-id");
                const commentItem = e.target.closest("li");

                try {
                    const response = await fetch(`/api/comment/${commentId}`, { method: "DELETE" });

                    if (response.ok) {
                        commentItem.remove(); // 댓글 삭제
                        console.log(`🗑 댓글 삭제 완료: ID ${commentId}`);
                    } else {
                        throw new Error("댓글 삭제 실패");
                    }
                } catch (error) {
                    console.error("❌ 댓글 삭제 실패:", error);
                    alert("댓글 삭제 실패: " + error.message);
                }
            }
        });
    }

    if (commentForm) {
        commentForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const postId = commentForm.querySelector("button[type='submit']").getAttribute("data-post-id");
            const content = commentForm.querySelector("input[name='content']").value.trim();

            if (!content) {
                alert("댓글 내용을 입력하세요!");
                return;
            }

            try {
                const response = await fetch(`/api/comment/${postId}`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ content }),
                });

                if (response.ok) {
                    const newComment = await response.json();
                    const newCommentElement = document.createElement("li");
                    newCommentElement.classList.add("comment");

                    const createdAt = new Date(newComment.createdAt).toISOString().split("T")[0];

                    newCommentElement.innerHTML = `
                        <div>
                          <span class="comment-username">${newComment.member.nickname}</span>
                            <span>${newComment.content}</span>
                            <button type="button" class="transparent-button" data-comment-id="${newComment.id}">삭제</button>
                        </div>
                        <div class="card-time">${createdAt}</div>              
                    `;

                    commentList.insertBefore(newCommentElement, commentList.firstChild);

                    commentForm.querySelector("input[name='content']").value = "";
                    commentForm.querySelector("input[name='content']").focus();
                } else if (response.status === 401) {
                    alert("로그인 후 댓글을 작성할 수 있습니다.");
                    window.location.href = "/members/login";
                } else {
                    throw new Error("댓글 생성 실패");
                }
            } catch (error) {
                console.error("❌ 댓글 생성 실패:", error);
                alert("댓글 생성 실패: " + error.message);
            }
        });
    }

}

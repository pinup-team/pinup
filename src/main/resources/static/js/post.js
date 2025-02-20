document.addEventListener('DOMContentLoaded', function () {
    const updatePostForm = document.getElementById('updatePostForm');
    if (updatePostForm) {
        updatePostForm.addEventListener('submit', function (event) {
            console.log("업데이트 처리해줘 ");
            event.preventDefault();

            const imagesToDelete = document.getElementById('imagesToDelete').value.split(',').filter(Boolean);
            const formData = new FormData(event.target);

            if (imagesToDelete.length > 0) {
                formData.set('imagesToDelete', imagesToDelete.join(','));
            }

            fetch('/api/post/' + formData.get('postId'), {
                method: 'PUT',
                body: formData,
            })
                .then(response => response.json())
                .then(data => {
                    if (data.id) {
                        alert("Post updated successfully!");
                        window.location.href = `/post/${data.id}`;
                    } else {
                        alert("Failed to update the post.");
                    }
                })
                .catch(error => {
                    console.error("Error deleting the post:", error);
                    alert("An error occurred while deleting the post.");
                });
        });
    }
});

document.addEventListener("DOMContentLoaded", function () {
    console.log("DOM fully loaded and parsed");
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
}

function submitPost() {
    const form = document.getElementById("postForm");
    const formData = new FormData(form);

    fetch("/api/post/create", {
        method: "POST",
        body: formData
    })
        .then(response => response.json())
        .then(data => {
            if (data.id) {
                alert("Post created successfully!");
                window.location.href = `/post/${data.id}`;
            } else {
                alert("Failed to create the post.");
            }
        })
        .catch(error => {
            console.error("Error creating post:", error);
            alert("An error occurred while creating the post.");
        });
}

function removePost(postId, storeId) {
    if (confirm("이 게시물을 삭제하시겠습니까?")) {
        fetch(`/api/post/${postId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    alert("Post deleted successfully!");
                    window.location.href = `/post/list/${storeId}`; // storeId를 여기서 사용
                } else {
                    alert("Failed to delete the post.");
                }
            })
            .catch(error => {
                console.error("Error deleting the post:", error);
                alert("An error occurred while deleting the post.");
            });
    }
}

// 파일 선택 시 파일명 표시 & 이미지 미리보기 기능 추가
function fileCheck(event) {
    const fileInput = event.target;
    const fileName = document.getElementById("fileName");
    const previewContainer = document.getElementById("previewContainer");

    previewContainer.innerHTML = ""; // 기존 미리보기 초기화

    const files = fileInput.files;

    if (files.length === 0) {
        fileName.innerText = "선택된 파일 없음";
        return;
    }

    fileName.innerText = `${files.length}개의 파일 선택됨`;

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
}

// 파일 업로드 버튼 클릭 시 input 트리거
function fileUpload() {
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
            console.error("Error loading detail:", error);
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

    // 🔥 댓글 삭제 이벤트 (이벤트 위임)
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

    // 🔥 댓글 작성 이벤트 바인딩
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
                            <span>사용자 이릅</span>
                            <span>${newComment.content}</span>
                            <button type="button" class="transparent-button" data-comment-id="${newComment.id}">삭제</button>
                        </div>
                        <div class="card-time">${createdAt}</div>              
                    `;

                    commentList.insertBefore(newCommentElement, commentList.firstChild);

                    commentForm.querySelector("input[name='content']").value = "";
                    commentForm.querySelector("input[name='content']").focus();

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

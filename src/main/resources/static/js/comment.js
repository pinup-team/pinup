document.addEventListener('DOMContentLoaded', () => {
    const commentList = document.getElementById('comment-list'); // 댓글 리스트

    commentList.addEventListener('click', async (e) => {

        if (e.target && e.target.classList.contains('delete-comment-btn')) {
            const commentId = e.target.getAttribute('data-comment-id');
            const commentItem = e.target.closest('li');

            try {
                const response = await fetch(`/api/comment/${commentId}`, {
                    method: 'DELETE'
                });

                if (response.ok) {
                    commentItem.remove(); // 삭제된 댓글을 화면에서 제거
                } else {
                    throw new Error('댓글 삭제 실패');
                }
            } catch (error) {
                console.error('댓글 삭제 실패:', error);
                alert("댓글 삭제 실패: " + error.message);
            }
        }
    });
});


document.addEventListener('DOMContentLoaded', () => {
    const commentForm = document.getElementById('comment-form');
    const commentList = document.getElementById('comment-list');

    if (commentForm) {
        commentForm.addEventListener('submit', async (e) => {
            e.preventDefault();

            const postId = commentForm.querySelector("button[type='submit']").getAttribute('data-post-id');
            const content = commentForm.querySelector("input[name='content']").value.trim();

            if (!content) {
                alert("댓글 내용을 입력하세요!");
                return;
            }

            try {
                const response = await fetch(`/api/comment/${postId}`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ content })
                });

                if (response.ok) {
                    const newComment = await response.json();

                    const newCommentElement = document.createElement('li');
                    newCommentElement.setAttribute('data-id', newComment.id);

                    const createdAt = new Date(newComment.createdAt);
                    const formattedDate = createdAt.toISOString().split('T')[0];

                    newCommentElement.innerHTML = `
                    <p>${newComment.content}</p>
                    <button type="button" class="delete-comment-btn" data-comment-id="${newComment.id}">삭제</button>
                    <div>${formattedDate}</div>              
                    `;

                    commentList.insertBefore(newCommentElement, commentList.firstChild);

                    commentForm.querySelector("input[name='content']").value = "";
                    commentForm.querySelector("input[name='content']").focus();
                } else {
                    throw new Error('댓글 생성 실패');
                }
            } catch (error) {
                console.error("댓글 생성 실패:", error);
                alert("댓글 생성 실패: " + error.message);
            }
        });
    }
});

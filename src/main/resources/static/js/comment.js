$(".delete-comment-btn").on("click", function(e) {
    e.preventDefault();
    console.log("삭제 버튼 클릭됨!");

    const commentId = parseInt($(this).data("comment-id"), 10);
    const $commentItem = $(this).closest("li");


    if (isNaN(commentId)) {
        console.error("잘못된 댓글 ID");
        return;
    }

    $.ajax({
        url: '/api/comment/' + commentId,
        type: 'DELETE',
        success: function(response) {
            console.log("삭제 성공:", response);
            $commentItem.remove();
        },
        error: function(xhr, status, error) {
            console.error("삭제 실패:", xhr.responseText);
            alert("댓글 삭제 실패: " + xhr.responseText);
        }
    });
});

$(document).ready(function() {

    $("#comment-form").on("submit", function(e) {
        e.preventDefault();

        const postId = parseInt($(this).find("button[type='submit']").data("post-id"), 10);

        if (isNaN(postId)) {
            console.error("postId 값이 유효하지 않습니다.");
            return;
        }

        const content = $("input[name='content']").val();
        if (!content.trim()) {
            alert("댓글 내용을 입력하세요!");
            return;
        }

        $.ajax({
            url: '/api/comment/' + postId,
            type: 'POST',
            contentType: 'application/json',
            data: JSON.stringify({ content }),
            success: function(response) {
                console.log("댓글 생성 성공:", response);

                const newCommentHtml = `
                    <li data-id="${response.id}" style="display: none;">
                        <p>${response.content}</p>
                        <button type="button" class="delete-comment-btn" data-comment-id="${response.id}">삭제</button>
                    </li>
                `;
                const $newComment = $(newCommentHtml);

                $("#comment-list").prepend($newComment);

                $newComment.fadeIn(500);

                $("input[name='content']").val("").focus();
            },
            error: function(xhr, status, error) {
                console.error("댓글 생성 실패:", xhr.responseText);
                alert("댓글 생성 실패: " + xhr.responseText);
            }
        });
    });
});
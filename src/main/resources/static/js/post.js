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

function toggleImageToDelete(checkboxElement) {
    const imageUrl = checkboxElement.getAttribute('data-image-url');
    const imagesToDeleteField = document.getElementById('imagesToDelete');

    let imagesToDelete = imagesToDeleteField.value ? new Set(imagesToDeleteField.value.split(',')) : new Set();

    if (checkboxElement.checked) {
        imagesToDelete.add(imageUrl);
    } else {
        imagesToDelete.delete(imageUrl);
    }
    imagesToDeleteField.value = Array.from(imagesToDelete).join(',');
}

document.getElementById('updatePostForm').addEventListener('submit', function (event) {
    event.preventDefault();
    const imagesToDelete = document.getElementById('imagesToDelete').value.split(',').filter(Boolean);

    const formData = new FormData(event.target);

    console.log("Images to delete:", imagesToDelete);

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

function removePost(postId) {
    if (confirm("Are you sure you want to delete this post?")) {
        fetch(`/api/post/${postId}`, {
            method: 'DELETE'
        })
            .then(response => {
                if (response.ok) {
                    alert("Post deleted successfully!");

                    window.location.href = `/post/list/${post.storeId}`;
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
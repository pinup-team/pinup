package kr.co.pinup.posts.service;

import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServicelTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageService postImageService;

    @DisplayName("게시물 생성 (이미지 포함)")
    @Test
    void testCreatePost() {
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("New Post")
                .content("This is a new post.")
                .build();

        MultipartFile[] images = new MultipartFile[] {
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "image_data".getBytes())
        };
        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .build();

        Post savedPost = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("New Post")
                .content("This is a new post.")
                .thumbnail("image_url")
                .build();

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(List.of(images))
                .build();

        when(postRepository.save(any(Post.class))).thenReturn(post);

        when(postImageService.savePostImages(any(PostImageRequest.class), any(Post.class)))
                .thenReturn(Collections.singletonList(new PostImage(post, "image_url")));

        PostResponse result = postService.createPost(createPostRequest, images);

        assertNotNull(result);
        assertEquals("New Post", result.title());
        assertEquals("image_url", result.thumbnail());

        verify(postRepository).save(any(Post.class));
        verify(postImageService).savePostImages(any(PostImageRequest.class), any(Post.class));
    }

    @DisplayName("Store ID에 대한 게시물 목록 조회")
    @Test
    void testFindByStoreId() {

        Long storeId = 1L;

        Post post = Post.builder()
                .storeId(storeId)
                .userId(1L)
                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findByStoreId(storeId)).thenReturn(Collections.singletonList(post));

        List<PostResponse> result = postService.findByStoreId(storeId);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Post Title", result.get(0).title());

        verify(postRepository).findByStoreId(storeId);
    }

    @DisplayName("게시물 ID로 게시물 조회")
    @Test
    void testGetPostById() {
        // Given
        Long postId = 1L;

        Post post = Post.builder()

                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        Post result = postService.getPostById(postId);

        assertNotNull(result);
        assertEquals("Post Title", result.getTitle());

        verify(postRepository).findById(postId);
    }
    @DisplayName("게시물 삭제(이미지 삭제 포함)")
    @Test
    void testDeletePost() {
        Long postId = 1L;

        Post post = Post.builder()
                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        postService.deletePost(postId);

        verify(postRepository).findById(postId);
        verify(postImageService).deleteAllByPost(postId);
        verify(postRepository).delete(post);
    }

    @DisplayName("게시물 수정 (이미지 삭제/업로드 포함)")
    @Test
    void testUpdatePostWithImages() {

        Long postId = 1L;

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post existingPost = Post.builder()
                .title("Old Title")
                .content("Old Content")
                .build();

        List<String> imagesToDelete = Collections.singletonList("image1_url");

        PostImageResponse remainingImage = PostImageResponse.builder()
                .s3Url("remaining_image_url")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postImageService.findImagesByPostId(postId)).thenReturn(Collections.singletonList(remainingImage));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        Post result = postService.updatePost(postId, updatePostRequest, new MultipartFile[0], imagesToDelete);

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());
        assertEquals("remaining_image_url", existingPost.getThumbnail());

        verify(postRepository).findById(postId);
        verify(postImageService).deleteSelectedImages(eq(postId), any(PostImageRequest.class));
        verify(postImageService).findImagesByPostId(postId);
        verify(postRepository).save(existingPost);
    }

    @DisplayName("게시물 수정 (이미지만 업로드)")
    @Test
    void testUpdatePostWithNewImages() {

        Long postId = 1L;

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post existingPost = Post.builder()
                .title("Old Title")
                .content("Old Content")
                .build();

        MultipartFile mockImage = mock(MultipartFile.class);
        MultipartFile[] newImages = {mockImage};

        PostImage uploadedImage = PostImage.builder()
                .s3Url("new_image_url")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postImageService.savePostImages(any(PostImageRequest.class), eq(existingPost)))
                .thenReturn(Collections.singletonList(uploadedImage));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        Post result = postService.updatePost(postId, updatePostRequest, newImages, Collections.emptyList());

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());
        assertEquals("new_image_url", existingPost.getThumbnail());

        verify(postRepository).findById(postId);
        verify(postImageService).savePostImages(any(PostImageRequest.class), eq(existingPost));
        verify(postRepository).save(existingPost);
    }

    @DisplayName("게시물 수정 (제목과 내용만 변경, 이미지 변경 없음)")
    @Test
    void testUpdatePostWithoutImageChanges() {

        Long postId = 1L;

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post existingPost = Post.builder()
                .title("Old Title")
                .content("Old Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        Post result = postService.updatePost(postId, updatePostRequest, new MultipartFile[0], Collections.emptyList());

        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());

        verify(postRepository).findById(postId);
        verify(postRepository).save(existingPost);

        verify(postImageService, never()).deleteSelectedImages(anyLong(), any(PostImageRequest.class));
        verify(postImageService, never()).findImagesByPostId(anyLong());
        verify(postImageService, never()).savePostImages(any(PostImageRequest.class), any(Post.class));
    }

    @DisplayName("게시물 수정 (모든 이미지 삭제 후 예외 발생)")
    @Test
    void testUpdatePostWithAllImagesDeleted() {
        Long postId = 1L;

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post existingPost = Post.builder()
                .title("Old Title")
                .content("Old Content")
                .build();

        List<String> imagesToDelete = Collections.singletonList("image1_url");

        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postImageService.findImagesByPostId(postId)).thenReturn(Collections.emptyList());

        assertThrows(PostImageNotFoundException.class, () -> {
            postService.updatePost(postId, updatePostRequest, new MultipartFile[0], imagesToDelete);
        });

        verify(postRepository).findById(postId);
        verify(postImageService).deleteSelectedImages(eq(postId), any(PostImageRequest.class));
        verify(postImageService).findImagesByPostId(postId);
        verify(postRepository, never()).save(existingPost);
    }

}

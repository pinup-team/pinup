package kr.co.pinup.posts.service;


import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PostServicelTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageService postImageService;

    @Test
    void testCreatePost() {
        // Given
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("New Post")
                .content("This is a new post.")
                .build();

        MultipartFile[] images = new MultipartFile[0];

        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title(createPostRequest.getTitle())
                .content(createPostRequest.getContent())
                .build();

        Post savedPost = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("New Post")
                .content("This is a new post.")
                .thumbnail("image_url")
                .build();

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Collections.emptyList())
                .build();

        // Mock repository save
        when(postRepository.save(any(Post.class))).thenReturn(post);

        // Mock postImageService
        when(postImageService.savePostImages(any(PostImageRequest.class), any(Post.class)))
                .thenReturn(Collections.singletonList(new PostImage(post, "image_url")));

        // When
        PostResponse result = postService.createPost(createPostRequest, images);

        // Then
        assertNotNull(result);
        assertEquals("New Post", result.getTitle());
        assertEquals("image_url", result.getThumbnail());

        verify(postRepository).save(any(Post.class));
        verify(postImageService).savePostImages(any(PostImageRequest.class), any(Post.class));
    }

    @Test
    void testFindByStoreId() {
        // Given
        Long storeId = 1L;

        Post post = Post.builder()
                .storeId(storeId)
                .userId(1L)
                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findByStoreId(storeId)).thenReturn(Collections.singletonList(post));

        // When
        List<PostResponse> result = postService.findByStoreId(storeId);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Post Title", result.get(0).getTitle());

        verify(postRepository).findByStoreId(storeId);
    }

    @Test
    void testGetPostById() {
        // Given
        Long postId = 1L;

        Post post = Post.builder()

                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        Post result = postService.getPostById(postId);

        // Then
        assertNotNull(result);
        assertEquals("Post Title", result.getTitle());

        verify(postRepository).findById(postId);
    }

    @Test
    void testDeletePost() {
        // Given
        Long postId = 1L;

        Post post = Post.builder()
                .title("Post Title")
                .content("Post Content")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        // When
        postService.deletePost(postId);

        // Then
        verify(postRepository).findById(postId);
        verify(postImageService).deleteAllByPost(postId);
        verify(postRepository).delete(post);
    }

    @Test
    void testUpdatePost() {
        // Given
        Long postId = 1L;

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post existingPost = Post.builder()
                .title("Old Title")
                .content("Old Content")
                .build();

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Collections.emptyList()) // 빈 이미지 리스트
                .imagesToDelete(Collections.singletonList("image1_url"))
                .build();

        PostImageResponse postImageResponse = PostImageResponse.builder()
                .s3Url("new_image_url")
                .build();

        // 기존 저장된 이미지를 삭제한 뒤 남아있는 이미지를 반환하도록 설정
        when(postRepository.findById(postId)).thenReturn(Optional.of(existingPost));
        when(postImageService.findImagesByPostId(postId)).thenReturn(Collections.singletonList(postImageResponse));
        when(postImageService.savePostImages(any(PostImageRequest.class), eq(existingPost)))
                .thenReturn(Collections.singletonList(PostImage.builder().s3Url("new_image_url").build())); // 새로운 이미지 추가 시 반환값 설정
        when(postRepository.save(any(Post.class))).thenReturn(existingPost);

        // When
        Post result = postService.updatePost(postId, updatePostRequest, new MultipartFile[0], Collections.singletonList("image1_url"));

        // Then
        assertNotNull(result);
        assertEquals("Updated Title", result.getTitle());
        assertEquals("Updated Content", result.getContent());

        verify(postRepository).findById(postId);
        verify(postImageService).deleteSelectedImages(eq(postId), any(PostImageRequest.class));
        verify(postImageService).findImagesByPostId(postId);
        verify(postRepository).save(existingPost);
    }

}

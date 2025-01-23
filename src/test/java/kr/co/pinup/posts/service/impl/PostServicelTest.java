package kr.co.pinup.posts.service.impl;


import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;

import kr.co.pinup.posts.Post;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;


import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class PostServicelTest {

    @InjectMocks
    private PostService postService;

    @Mock
    private PostRepository postRepository;

    @Mock
    private PostImageService postImageService;

    private Post post;

    @BeforeEach
    void setUp() {
        post = Post.builder()
                .storeId(123L)
                .userId(456L)
                .title("Test Post")
                .content("This is a test post.")
                .thumbnail("http://example.com/image1.jpg")
                .build();
    }

    @Test
    void testCreatePost() throws IOException {
        // CreatePostRequest 빌더 패턴 사용
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("New Post")
                .content("This is a new post")
                .storeId(1L)
                .userId(1L)
                .build();

        // MultipartFile 배열 생성
        MultipartFile[] images = new MultipartFile[]{
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes()),
                new MockMultipartFile("image2", "image2.jpg", "image/jpeg", "dummy content".getBytes())
        };

        // PostImageRequest 빌더 패턴 사용
        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .build();

        List<PostImage> postImageEntities = Arrays.stream(images)
                .map(multipartFile -> PostImage.builder()
                        .post(post)
                        .s3Url("http://example.com/" + multipartFile.getOriginalFilename())
                        .build())
                .collect(Collectors.toList());

        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postImageService.savePostImages(any(PostImageRequest.class), any(Post.class)))
                .thenReturn(postImageEntities);

        Post savedPost = postService.createPost(createPostRequest, images);

        verify(postImageService, times(1)).savePostImages(any(PostImageRequest.class), any(Post.class));

        assertNotNull(savedPost.getThumbnail());
        assertEquals("http://example.com/image1.jpg", savedPost.getThumbnail());
    }

    @Test
    void testGetPostById() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        Post result = postService.getPostById(1L);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("Test Post", result.getTitle());
        assertEquals("This is a test post.", result.getContent());
        assertEquals("http://example.com/image1.jpg", result.getThumbnail());
    }

    @Test
    void testGetPostByIdNotFound() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> {
            postService.getPostById(1L);
        });
    }

    @Test
    void testDeletePost() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        doNothing().when(postRepository).delete(post);
        doNothing().when(postImageService).deleteAllByPost(1L);

        postService.deletePost(1L);

        verify(postRepository, times(1)).delete(post);
        verify(postImageService, times(1)).deleteAllByPost(1L);
    }

    @Test
    void testUpdatePost() throws IOException {
        // given
        Post post = Post.builder()
                .id(1L)  // ID를 명시적으로 설정합니다.
                .title("Test Post")
                .content("Test Content")
                .thumbnail("thumbnail.png")
                .storeId(1L)
                .userId(1L)
                .build();

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Post")
                .content("This is an updated post")
                .postImageRequest(PostImageRequest.builder()
                        .images(Arrays.asList(
                                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes())
                        ))
                        .build())
                .build();

        MultipartFile[] images = new MultipartFile[]{
                new MockMultipartFile("image1", "image1.jpg", "image/jpeg", "dummy content".getBytes())
        };

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .build();

        List<PostImage> postImageEntities = Arrays.stream(images)
                .map(multipartFile -> PostImage.builder()
                        .post(post)
                        .s3Url("http://example.com/" + multipartFile.getOriginalFilename())
                        .build())
                .collect(Collectors.toList());

        // Mock 서비스 메서드
        when(postRepository.findById(anyLong())).thenReturn(Optional.of(post));
        when(postRepository.save(any(Post.class))).thenReturn(post);
        when(postImageService.savePostImages(any(PostImageRequest.class), any(Post.class)))
                .thenReturn(postImageEntities);

        // when
        Post updatedPost = postService.updatePost(post.getId(), updatePostRequest, images);

        // then
        verify(postImageService, times(1)).savePostImages(any(PostImageRequest.class), any(Post.class));
        assertNotNull(updatedPost.getThumbnail());
        assertEquals("http://example.com/image1.jpg", updatedPost.getThumbnail());
    }

}
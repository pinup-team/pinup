package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PostApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private PostImageService postImageService;
    @TestConfiguration
    static class PostApiControllerTestConfig {
        @Bean
        public PostService postService() {
            return mock(PostService.class);
        }

        @Bean
        public CommentService commentService() {
            return mock(CommentService.class);
        }

        @Bean
        public PostImageService postImageService() {
            return mock(PostImageService.class);
        }
    }
    @Test
    void testGetAllPosts() throws Exception {
        // given
        Long storeId = 1L;
        List<PostResponse> postResponses = List.of(
                PostResponse.builder()
                        .id(1L)
                        .title("Test Title 1")
                        .content("Test Content 1")
                        .thumbnail("thumbnail1.png")
                        .storeId(1L)
                        .userId(1L)
                        .build(),
                PostResponse.builder()
                        .id(2L)
                        .title("Test Title 2")
                        .content("Test Content 2")
                        .thumbnail("thumbnail2.png")
                        .storeId(1L)
                        .userId(2L)
                        .build()
        );

        when(postService.findByStoreId(storeId)).thenReturn(postResponses);

        // when & then
        mockMvc.perform(get("/api/post/list/{storeid}", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Test Title 1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Test Title 2"));
    }

    @Test
    void testGetPostById() throws Exception {
        // given
        Long postId = 1L;
        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .thumbnail("thumbnail.png")
                .storeId(1L)
                .userId(1L)
                .build();
        List<CommentResponse> commentResponses = List.of(
                CommentResponse.builder()
                        .id(1L)
                        .userId(1L)
                        .content("Test Comment 1")
                        .build(),
                CommentResponse.builder()
                        .id(2L)
                        .userId(1L)
                        .content("Test Comment 2")
                        .build()
        );

        List<PostImageResponse> imageResponses = List.of(
                PostImageResponse.builder()
                        .id(1L)
                        .s3Url("image2.png")
                        .build(),
                PostImageResponse.builder()
                        .id(2L)
                        .s3Url("image2.png")
                        .build()
        );


        when(postService.getPostById(postId)).thenReturn(post);
        when(commentService.findByPostId(postId)).thenReturn(commentResponses);
        when(postImageService.findImagesByPostId(postId)).thenReturn(imageResponses);

        // when & then
        mockMvc.perform(get("/api/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Test Post"))
                .andExpect(jsonPath("$.comments[0].content").value("Test Comment 1"))
                .andExpect(jsonPath("$.postImages[0].imageUrl").value("image2.png"));
    }

    @Test
    void testCreatePost() throws Exception {
        // given
        MockMultipartFile image1 = new MockMultipartFile("images", "image1.png", "image/png", "test image 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.png", "image/png", "test image 2".getBytes());
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("Title")
                .content("Content")
                .storeId(1L)
                .userId(1L)
                .build();

        Post createdPost = Post.builder()
                .title("Title")
                .content("Content")
                .storeId(1L)
                .userId(1L)
                .build();

        when(postService.createPost(any(CreatePostRequest.class), any(MultipartFile[].class)))
                .thenReturn(createdPost);

        // when & then
        mockMvc.perform(multipart("/api/post/create")
                        .file(image1)
                        .file(image2)
                        .param("title", "Title")
                        .param("content", "Content")
                        .param("storeId", "1")
                        .param("userId", "1"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Title"));
    }

    @Test
    void testDeletePost() throws Exception {
        // given
        Long postId = 1L;

        // when & then
        mockMvc.perform(delete("/api/post/{id}", postId))
                .andExpect(status().isNoContent());

        verify(postService).deletePost(postId);
    }

    @Test
    void testUpdatePost() throws Exception {
        // given
        Long postId = 1L;
        MockMultipartFile image1 = new MockMultipartFile("images", "image1.png", "image/png", "test image 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.png", "image/png", "test image 2".getBytes());
        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post updatedPost = Post.builder()
                .title("Updated Title")
                .content("Updated Content")
                .storeId(1L)
                .userId(1L)
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), any(MultipartFile[].class)))
                .thenReturn(updatedPost);

        // when & then
        mockMvc.perform(multipart("/api/post/{id}", postId)
                        .file(image1)
                        .file(image2)
                        .param("title", "Updated Title")
                        .param("content", "Updated Content")
                        .with(request -> {
                            request.setMethod("PUT"); // MockMvc로 PUT 요청 처리
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Updated Title"));
    }
}


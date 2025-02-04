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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @InjectMocks
    private PostApiController postApiController;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postApiController).build();
    }

    @DisplayName("게시글 목록 조회")
    @Test
    void testGetAllPosts() throws Exception {
        PostResponse postResponse = PostResponse.builder()
                .id(1L)
                .title("Title 1")
                .content("Content 1")
                .build();

        List<PostResponse> postResponses = List.of(postResponse);

        when(postService.findByStoreId(1L)).thenReturn(postResponses);

        mockMvc.perform(get("/api/post/list/1"))
                .andExpect(status().isOk())
                .andDo(print())
                .andExpect(content().json("[{'id': 1, 'title': 'Title 1', 'content': 'Content 1'}]"));
    }
    @DisplayName("게시글 ID로 조회")
    @Test
    void testGetPostById() throws Exception {
        Long postId = 1L;

        Post post = Post.builder()
                .userId(1L)
                .storeId(1L)
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        List<CommentResponse> comments = List.of(CommentResponse.builder().id(1L).content("Comment 1").build());
        List<PostImageResponse> images = List.of(PostImageResponse.builder().s3Url("image1.jpg").build());

        when(postService.getPostById(postId)).thenReturn(post);
        when(commentService.findByPostId(postId)).thenReturn(comments);
        when(postImageService.findImagesByPostId(postId)).thenReturn(images);

        mockMvc.perform(get("/api/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Title 1"))
                .andExpect(jsonPath("$.content").value("Content 1"))
                .andExpect(jsonPath("$.thumbnail").value("Thumbnail"))
                .andExpect(jsonPath("$.comments[0].content").value("Comment 1"))
                .andExpect(jsonPath("$.postImages[0].s3Url").value("image1.jpg"));
    }

    @DisplayName("게시글 생성")
    @Test
    void testCreatePost() throws Exception {
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();
        PostResponse createdPostResponseDto = PostResponse.builder()
                .id(1L)
                .storeId(1L)
                .userId(1L)
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        when(postService.createPost(any(CreatePostRequest.class), any(MultipartFile[].class))).thenReturn(createdPostResponseDto);

        mockMvc.perform(multipart("/api/post/create")
                        .file("images", new byte[]{})
                        .param("title", "Title 1")
                        .param("content", "Content 1")
                        .param("thumbnail", "Thumbnail"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Title 1"))
                .andExpect(jsonPath("$.content").value("Content 1"))
                .andExpect(jsonPath("$.thumbnail").value("Thumbnail"));
    }

    @DisplayName("게시글 삭제")
    @Test
    void testDeletePost() throws Exception {
        Long postId = 1L;

        doNothing().when(postService).deletePost(postId);

        mockMvc.perform(delete("/api/post/{id}", postId))
                .andExpect(status().isNoContent());
    }

    @DisplayName("게시글 수정")
    @Test
    void testUpdatePost() throws Exception {
        Long postId = 1L;
        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();
        Post updatedPost = Post.builder()
                .userId(1L)
                .storeId(1L)
                .title("Updated Title")
                .content("Updated Content")
                .thumbnail("Updated Thumbnail")
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), any(MultipartFile[].class), anyList()))
                .thenReturn(updatedPost);

        MockMultipartFile images = new MockMultipartFile(
                "images",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{}
        );

        mockMvc.perform(multipart("/api/post/{id}", postId)
                        .file(images)
                        .param("title", "Updated Title")
                        .param("content", "Updated Content")
                        .param("imagesToDelete", "imageToDelete")
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.thumbnail").value("Updated Thumbnail"));
    }

}

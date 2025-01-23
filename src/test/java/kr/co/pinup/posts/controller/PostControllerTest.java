package kr.co.pinup.posts.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.service.PostService;

import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;

import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
class PostControllerTest {

    @InjectMocks
    private PostController postController;

    @Mock
    private PostService postService;

    @Mock
    private CommentService commentService;

    @Mock
    private PostImageService postImageService;

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    static class PostControllerTestConfig {
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
        Long storeId = 1L;
        List<Post> posts = new ArrayList<>();
        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("Test Post Title")
                .content("Test post content.")
                .thumbnail("thumbnail_url")
                .build();

        // Mock service call
        when(postService.findByStoreId(storeId)).thenReturn((List<PostResponse>) post);

        // Perform GET request and validate response
        mockMvc.perform(get("/post/list/{storeid}", storeId))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(view().name("views/post/list")) // Expected view name
                .andExpect(model().attributeExists("posts")) // "posts" attribute should exist in the model
                .andExpect(model().attribute("posts", posts)); // Validate the "posts" attribute
    }

    @Test
    void testGetPostById() throws Exception {
        Long postId = 1L;
        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("Test Post Title")
                .content("Test post content.")
                .thumbnail("thumbnail_url")
                .build();
        // 댓글 리스트를 준비
        Comment comment = Comment.builder()
                .post(post)
                .content("Test comment")
                .build();
        // CommentResponse를 수동으로 생성
        CommentResponse commentResponse =  CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .build();

        // 댓글 리스트를 생성
        List<CommentResponse> comments = List.of(commentResponse);

        // 게시물 이미지 리스트를 준비
        PostImage postImage = PostImage.builder()
                .post(post)
                .s3Url("image_url")
                .build();

        List<PostImageResponse> images = List.of(PostImageResponse.from(postImage));


        // Mock service calls
        when(postService.getPostById(postId)).thenReturn(post);
        when(commentService.findByPostId(postId)).thenReturn(comments);
        when(postImageService.findImagesByPostId(postId)).thenReturn(images);

        // Perform GET request and validate response
        mockMvc.perform(get("/post/{postId}", postId))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(view().name("views/post/detail")) // Expected view name
                .andExpect(model().attributeExists("post")) // "post" attribute should exist in the model
                .andExpect(model().attributeExists("comments")) // "comments" attribute should exist in the model
                .andExpect(model().attributeExists("images")); // "images" attribute should exist in the model
    }

    @Test
    void testCreatePostForm() throws Exception {
        // Perform GET request and validate response
        mockMvc.perform(get("/post/create"))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(view().name("views/post/create")) // Expected view name
                .andExpect(model().attributeExists("createPostRequest")); // "createPostRequest" should exist in the model
    }

    @Test
    void testUpdatePostForm() throws Exception {
        Long postId = 1L;
        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("Test Post Title")
                .content("Test post content.")
                .thumbnail("thumbnail_url")
                .build();

        PostImage postImage = PostImage.builder()
                .post(post)
                .s3Url("image_url")
                .build();

        // ImageResponse 객체 생성
        List<PostImageResponse> images = List.of( PostImageResponse.builder()
                .s3Url(postImage.getS3Url())
                .build());

        when(postService.getPostById(postId)).thenReturn(post);
        when(postImageService.findImagesByPostId(postId)).thenReturn(images);


        // Perform GET request and validate response
        mockMvc.perform(get("/post/update/{id}", postId))
                .andExpect(status().isOk()) // 200 OK
                .andExpect(view().name("views/post/update")) // Expected view name
                .andExpect(model().attributeExists("post")) // "post" attribute should exist in the model
                .andExpect(model().attributeExists("images")); // "images" attribute should exist in the model
    }

}

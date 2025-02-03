package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.PostResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @DisplayName("게시글 목록 조회 페이지 로드")
    @Test
    void testGetAllPosts() throws Exception {
        Long storeId = 1L;

        List<PostResponse> posts = List.of(
                PostResponse.builder()
                        .id(1L)
                        .storeId(storeId)
                        .userId(1L)
                        .title("Test Post Title")
                        .content("Test post content.")
                        .thumbnail("thumbnail_url")
                        .build()
        );

        when(postService.findByStoreId(storeId)).thenReturn(posts);

        mockMvc.perform(get("/post/list/{storeid}", storeId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/list"))
                .andExpect(model().attributeExists("posts"))
                .andExpect(model().attribute("posts", posts));
    }

    @DisplayName("게시글 상세 조회 페이지 로드")
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
        Comment comment = Comment.builder()
                .post(post)
                .content("Test comment")
                .build();
        CommentResponse commentResponse =  CommentResponse.builder()
                .id(comment.getId())
                .content(comment.getContent())
                .postId(comment.getPost().getId())
                .build();

        List<CommentResponse> comments = List.of(commentResponse);

        PostImage postImage = PostImage.builder()
                .post(post)
                .s3Url("image_url")
                .build();

        List<PostImageResponse> images = List.of(PostImageResponse.from(postImage));

        when(postService.getPostById(postId)).thenReturn(post);
        when(commentService.findByPostId(postId)).thenReturn(comments);
        when(postImageService.findImagesByPostId(postId)).thenReturn(images);

        mockMvc.perform(get("/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/detail"))
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("comments"))
                .andExpect(model().attributeExists("images"));
    }

    @DisplayName("게시글 생성 폼 페이지 로드")
    @Test
    void testCreatePostForm() throws Exception {
        mockMvc.perform(get("/post/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/create"))
                .andExpect(model().attributeExists("createPostRequest"));
    }

    @DisplayName("게시글 수정 폼 페이지 로드")
    @Test
    void testUpdatePostForm() throws Exception {
        Long postId = 1L;

        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title("Updated Post Title")
                .content("Updated post content.")
                .thumbnail("thumbnail_url")
                .build();

        List<PostImageResponse> images = List.of(
                PostImageResponse.builder()
                        .s3Url("image1_url")
                        .build(),
                PostImageResponse.builder()
                        .s3Url("image2_url")
                        .build()
        );

        when(postService.getPostById(postId)).thenReturn(post);
        when(postImageService.findImagesByPostId(postId)).thenReturn(images);

        mockMvc.perform(get("/post/update/{id}", postId))
                .andExpect(status().isOk())
                .andExpect(view().name("views/posts/update"))
                .andExpect(model().attributeExists("post"))
                .andExpect(model().attributeExists("images"))
                .andExpect(model().attribute("post", post))
                .andExpect(model().attribute("images", images));
    }

}

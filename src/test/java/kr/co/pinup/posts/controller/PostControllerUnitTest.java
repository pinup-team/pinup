package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PostControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private PostImageService postImageService;
    @Mock
    private MemberService memberService;

    @InjectMocks
    private PostController postController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    @DisplayName("게시물 리스트 페이지 이동 - 성공")
    void listPage_whenCalled_thenReturnsPostListView() throws Exception {
        // Given
        Long storeId = 1L;
        when(postService.findByStoreIdWithCommentCount(storeId, false)).thenReturn(List.of());

        // When
        ResultActions result = mockMvc.perform(get("/post/list/{storeId}", storeId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/list"))
                .andExpect(model().attributeExists("posts", "storeId"));
    }

    @Test
    @DisplayName("게시물 상세 페이지 이동 - 성공")
    void detailPage_whenExistingPost_thenReturnsPostDetailView() throws Exception {
        // Given
        Long postId = 1L;

        PostResponse postResponse = mock(PostResponse.class);
        when(postService.getPostById(postId, false)).thenReturn(postResponse);
        when(commentService.findByPostId(postId)).thenReturn(List.of());
        when(postImageService.findImagesByPostId(postId)).thenReturn(List.of());

        // When
        ResultActions result = mockMvc.perform(get("/post/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/detail"))
                .andExpect(model().attributeExists("post", "comments", "images"));
    }

    @Test
    @DisplayName("게시물 생성 페이지 이동 - 성공")
    void createPage_whenCalled_thenReturnsPostCreateView() throws Exception {
        // Given
        String storeId = "1";

        // When
        ResultActions result = mockMvc.perform(get("/post/create").param("storeId", storeId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/create"))
                .andExpect(model().attributeExists("storeId"));
    }

    @Test
    @DisplayName("게시물 수정 페이지 이동 - 성공")
    void updatePage_whenExistingPost_thenReturnsPostUpdateView() throws Exception {
        // Given
        Long postId = 1L;

        PostResponse postResponse = mock(PostResponse.class);
        when(postService.getPostById(postId, false)).thenReturn(postResponse);
        when(postImageService.findImagesByPostId(postId)).thenReturn(List.of());

        // When
        ResultActions result = mockMvc.perform(get("/post/update/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/update"))
                .andExpect(model().attributeExists("post", "images"));
    }
}

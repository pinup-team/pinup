package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.ImageCountException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.service.PostService;
import kr.co.pinup.stores.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class PostApiControllerUnitTest {

    private MockMvc mockMvc;

    @Mock
    private PostService postService;
    @Mock
    private CommentService commentService;
    @Mock
    private PostImageService postImageService;

    @InjectMocks
    private PostApiController postController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(postController).build();
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    void getPosts_whenPostsExist_thenReturnsPostList() throws Exception {
        // Given
        PostResponse postResponse = PostResponse.builder()
                .id(1L)
                .title("Title 1")
                .content("Content 1")
                .build();

        when(postService.findByStoreId(1L, false)).thenReturn(List.of(postResponse));

        // When
        ResultActions result = mockMvc.perform(get("/api/post/list/1"));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().json("[{'id': 1, 'title': 'Title 1', 'content': 'Content 1'}]"));
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    void getPostById_whenPostExists_thenReturnsPostDetail() throws Exception {
        // Given
        Long postId = 1L;
        Member member = Member.builder().nickname("행복한돼지").build();
        Store store = Store.builder().name("Test Store").build();

        Post post = Post.builder()
                .store(store)
                .member(member)
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        when(postService.getPostById(postId, false)).thenReturn(PostResponse.from(post));
        when(commentService.findByPostId(postId)).thenReturn(List.of());
        when(postImageService.findImagesByPostId(postId)).thenReturn(List.of());

        // When
        ResultActions result = mockMvc.perform(get("/api/post/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("게시글 생성 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createPost_whenAuthenticated_givenValidRequest_thenSuccess() throws Exception {
        // Given
        when(postService.createPost(any(MemberInfo.class), any(CreatePostRequest.class), any(MultipartFile[].class)))
                .thenReturn(PostResponse.builder().id(1L).storeId(1L).title("Title 1").content("Content 1").thumbnail("Thumbnail").build());

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image content 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image content 2".getBytes());

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/create")
                .file(image1).file(image2)
                .param("storeId", "1")
                .param("title", "Title 1")
                .param("content", "Content 1")
                .param("thumbnail", "Thumbnail"));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.storeId").value(1L))
                .andExpect(jsonPath("$.title").value("Title 1"))
                .andExpect(jsonPath("$.content").value("Content 1"))
                .andExpect(jsonPath("$.thumbnail").value("Thumbnail"));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 이미지 부족")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createPost_whenInsufficientImages_thenThrowsException() throws Exception {
        // Given
        MockMultipartFile image = new MockMultipartFile("images", "image.jpg", "image/jpeg", "image content".getBytes());

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/create")
                .file(image)
                .param("storeId", "1")
                .param("title", "Title 1")
                .param("content", "Content 1")
                .param("thumbnail", "Thumbnail"));

        // Then
        result.andExpect(status().isBadRequest())
                .andExpect(r -> assertTrue(r.getResolvedException() instanceof ImageCountException));
    }

    @Test
    @DisplayName("게시글 삭제 - 성공")
    void deletePost_whenExistingPost_thenNoContent() throws Exception {
        // Given
        doNothing().when(postService).deletePost(1L);

        // When
        ResultActions result = mockMvc.perform(delete("/api/post/{id}", 1L));

        // Then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("게시글 수정 - 성공")
    void updatePost_whenValidRequest_thenSuccess() throws Exception {
        // Given
        Store mockStore = Store.builder().name("Mock Store").build();
        Member mockMember = Member.builder().nickname("행복한돼지").build();

        Post updatedPost = Post.builder()
                .title("Updated Title")
                .content("Updated Content")
                .thumbnail("Updated Thumbnail")
                .store(mockStore)  // <- Store 설정 추가
                .member(mockMember)
                .build();

        when(postService.updatePost(eq(1L), any(UpdatePostRequest.class), any(MultipartFile[].class), anyList()))
                .thenReturn(updatedPost);

        MockMultipartFile images = new MockMultipartFile(
                "images",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{}
        );

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/{postId}", 1L)
                .file(images)
                .param("title", "Updated Title")
                .param("content", "Updated Content")
                .param("imagesToDelete", "imageToDelete")
                .with(req -> {
                    req.setMethod("PUT");
                    return req;
                }));

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.thumbnail").value("Updated Thumbnail"));
    }

}

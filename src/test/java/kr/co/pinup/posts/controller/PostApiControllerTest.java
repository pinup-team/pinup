package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.locations.Location;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.service.PostService;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@ActiveProfiles("test")
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

    Member member;

    Store store;

    @BeforeEach
    void setUp() {
        member = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("hdiJZoHQ-XDUkGvVCDLr1_NnTNZGcJjyxSAEUFjEi6A")
                .role(MemberRole.ROLE_USER)
                .build();

        store = Store.builder()
                .name("Test Store")
                .description("Description of the store")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(Status.RESOLVED)
                .imageUrl("image_url")
                .category(new StoreCategory("Category Name"))
                .location(new Location("Test Location","12345","Test State","Test District",37.7749,-122.4194,"1234 Test St.", "Suite 101"))
                .build();

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
                .store(store)
                .member(member)
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
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testCreatePost() throws Exception {

        MemberResponse memberResponse = new MemberResponse(member);

        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .title("Title 1")
                .content("Content 1")
                .build();
        PostResponse createdPostResponseDto = PostResponse.builder()
                .id(1L)
                .storeId(1L)
                .member(memberResponse)
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        when(postService.createPost(any(MemberInfo.class), any(CreatePostRequest.class), any(MultipartFile[].class)))
                .thenReturn(createdPostResponseDto);

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

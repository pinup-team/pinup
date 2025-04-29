package kr.co.pinup.posts.controller;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.posts.service.PostService;
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(PostApiControllerIntegrationTest.MockServiceTestConfig.class)
public class PostApiControllerIntegrationTest {

    @TestConfiguration
    static class MockServiceTestConfig {
        @Bean
        public MemberService memberService() {
            return mock(MemberService.class);
        }

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

    @Autowired private MockMvc mockMvc;

    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;

    @Autowired private MemberService memberService;
    @Autowired private PostService postService;
    @Autowired private CommentService commentService;
    @Autowired private PostImageService postImageService;

    private Member mockMember;
    private Member mockAdminMember;
    private Post mockPost;
    private Comment mockComment;
    private PostImage mockPostImage;
    private Store mockStore;

    @BeforeEach
    public void setUp() {
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(false);
        when(memberService.refreshAccessToken(any())).thenReturn("mocked-token");

        StoreCategory category = new StoreCategory("Category Name");
        StoreCategory savedCategory = storeCategoryRepository.save(category);

        Location location = new Location("Test Location", "12345", "Test State", "Test District", 37.7749, -122.4194, "1234 Test St.", "Suite 101");
        Location savedLocation = locationRepository.save(location);

        mockStore = Store.builder()
                .name("Test Store")
                .description("Description of the store")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(Status.RESOLVED)
                .imageUrl("image_url")
                .category(savedCategory)
                .location(savedLocation)
                .build();

        storeRepository.save(mockStore);

        mockMember = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("hdiJZoHQ-XDUkGvVCDLr1_NnTNZGcJjyxSAEUFjEi6A")
                .role(MemberRole.ROLE_USER)
                .build();

        mockAdminMember = Member.builder()
                .email("admin@naver.com")
                .name("admin")
                .nickname("행복한 관리자")
                .providerType(OAuthProvider.NAVER)
                .providerId("adminProviderId12345")
                .role(MemberRole.ROLE_ADMIN)
                .build();
        memberRepository.save(mockAdminMember);
        memberRepository.save(mockMember);

        mockPost = Post.builder()
                .title("게시물 제목")
                .content("게시물 내용")
                .store(mockStore)
                .member(mockMember)
                .build();
        postRepository.save(mockPost);

        mockComment = Comment.builder()
                .post(mockPost)
                .content("댓글 내용")
                .member(mockMember)
                .build();
        commentRepository.save(mockComment);

        mockPostImage = PostImage.builder()
                .post(mockPost)
                .s3Url("image.jpg")
                .build();
        postImageRepository.save(mockPostImage);
    }

    @Test
    @DisplayName("게시글 목록 조회 - 성공")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void getPosts_whenPostsExist_thenReturnsPostList() throws Exception {
        // Given
        PostResponse postResponse = PostResponse.builder()
                .id(mockPost.getId())
                .title(mockPost.getTitle())
                .content(mockPost.getContent())
                .build();

        when(postService.findByStoreId(mockStore.getId(), false)).thenReturn(List.of(postResponse));

        // When
        ResultActions result = mockMvc.perform(get("/api/post/list/{storeId}", mockStore.getId()));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().json("[{'id': " + mockPost.getId() + ", 'title': '" + mockPost.getTitle() + "', 'content': '" + mockPost.getContent() + "'}]"));
    }

    @Test
    @DisplayName("게시글 ID로 조회 - 성공")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void getPostById_whenPostExists_thenReturnsPostDetail() throws Exception {
        // Given
        Long postId = mockPost.getId();

        when(postService.getPostById(postId, false)).thenReturn(PostResponse.from(mockPost));
        when(commentService.findByPostId(postId)).thenReturn(List.of(new CommentResponse(mockComment.getId(), postId, mockMember, mockComment.getContent(), LocalDateTime.now())));
        when(postImageService.findImagesByPostId(postId)).thenReturn(List.of(new PostImageResponse(mockPostImage.getId(), mockPost.getId(), mockPostImage.getS3Url())));

        // When
        ResultActions result = mockMvc.perform(get("/api/post/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @Test
    @DisplayName("게시글 생성 - 인증된 사용자 성공")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createPost_whenAuthenticated_givenValidRequest_thenSuccess() throws Exception {
        // Given
        PostResponse createdPostResponse = PostResponse.builder()
                .id(1L)
                .storeId(mockStore.getId())
                .member(new MemberResponse(mockMember))
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        when(postService.createPost(any(MemberInfo.class), any(CreatePostRequest.class), any(CreatePostImageRequest.class)))
                .thenReturn(createdPostResponse);

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image content 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image content 2".getBytes());

        CreatePostRequest postRequest = new CreatePostRequest(
                mockStore.getId(),
                "Title 1",
                "Content 1"
        );

        String postJson = new ObjectMapper().writeValueAsString(postRequest);

        MockMultipartFile postPart = new MockMultipartFile(
                "post", "post.json", "application/json", postJson.getBytes(StandardCharsets.UTF_8)
        );

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/create")
                .file(image1)
                .file(image2)
                .file(postPart)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .characterEncoding("UTF-8"));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.storeId").value(mockStore.getId()))
                .andExpect(jsonPath("$.title").value("Title 1"))
                .andExpect(jsonPath("$.content").value("Content 1"))
                .andExpect(jsonPath("$.thumbnail").value("Thumbnail"));
    }

    @Test
    @DisplayName("게시글 생성 실패 - 이미지 부족")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createPost_whenInsufficientImages_thenThrowsImageCountException() throws Exception {
        // Given
        CreatePostRequest postRequest = new CreatePostRequest(
                mockStore.getId(),
                "Title 1",
                "Content 1"
        );
        String postJson = new ObjectMapper().writeValueAsString(postRequest);

        MockMultipartFile postPart = new MockMultipartFile(
                "post", "post.json", "application/json", postJson.getBytes(StandardCharsets.UTF_8)
        );

        MockMultipartFile image = new MockMultipartFile(
                "images", "image.jpg", "image/jpeg", "image content".getBytes()
        );

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/create")
                .file(postPart)
                .file(image)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .characterEncoding("UTF-8"));

        // Then
        result.andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("게시글 삭제 - 관리자 성공")
    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    void deletePost_whenAdminUser_givenExistingPost_thenNoContent() throws Exception {
        // Given
        Long postId = mockPost.getId();
        doNothing().when(postService).deletePost(postId);

        // When
        ResultActions result = mockMvc.perform(delete("/api/post/{id}", postId));

        // Then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("게시글 수정 - 인증된 사용자 성공")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void updatePost_whenAuthenticated_givenValidRequest_thenSuccess() throws Exception {
        // Given
        Long postId = mockPost.getId();

        UpdatePostRequest updatePostRequest = UpdatePostRequest.builder()
                .title("Updated Title")
                .content("Updated Content")
                .build();

        Post updatedPost = Post.builder()
                .title("Updated Title")
                .content("Updated Content")
                .thumbnail("Updated Thumbnail")
                .store(mockStore)
                .member(mockMember)
                .build();

        when(postService.updatePost(eq(postId), any(UpdatePostRequest.class), any(MultipartFile[].class), anyList()))
                .thenReturn(PostResponse.from(updatedPost));

        MockMultipartFile image = new MockMultipartFile(
                "images",
                "test-image.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        // 핵심: UpdatePostRequest JSON으로 변환 후 RequestPart로 전달
        String json = new ObjectMapper().writeValueAsString(updatePostRequest);
        MockMultipartFile updatePostRequestPart = new MockMultipartFile(
                "updatePostRequest",
                "updatePostRequest.json",
                "application/json",
                json.getBytes(StandardCharsets.UTF_8)
        );

        // When
        ResultActions result = mockMvc.perform(multipart("/api/post/{postId}", postId)
                        .file(image)
                        .file(updatePostRequestPart)
                        .param("imagesToDelete", "imageToDelete")
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .characterEncoding("UTF-8")
                        .with(request -> {
                            request.setMethod("PUT"); // PUT으로 강제 설정
                            return request;
                        }))
                .andDo(print());

        // Then
        result.andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.content").value("Updated Content"))
                .andExpect(jsonPath("$.thumbnail").value("Updated Thumbnail"));
    }

    @Test
    @DisplayName("게시글 비활성화 - 인증된 사용자 성공")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void disablePost_whenAuthenticatedUser_givenExistingPost_thenNoContent() throws Exception {
        // Given
        Long postId = mockPost.getId();
        doNothing().when(postService).disablePost(postId);

        // When
        ResultActions result = mockMvc.perform(patch("/api/post/{postId}/disable", postId));

        // Then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("게시글 비활성화 - 관리자 성공")
    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    void disablePost_whenAdminUser_givenExistingPost_thenNoContent() throws Exception {
        // Given
        Long postId = mockPost.getId();
        doNothing().when(postService).disablePost(postId);

        // When
        ResultActions result = mockMvc.perform(patch("/api/post/{postId}/disable", postId));

        // Then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("게시글 비활성화 실패 - 인증되지 않은 사용자")
    void disablePost_whenUnauthenticated_thenUnauthorized() throws Exception {
        // Given
        Long postId = mockPost.getId();

        // When
        ResultActions result = mockMvc.perform(patch("/api/post/{postId}/disable", postId));

        // Then
        result.andExpect(status().isUnauthorized());
    }

}

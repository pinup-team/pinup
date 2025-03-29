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
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.ImageCountException;
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
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(PostApiControllerIntegrationTest.MockServiceTestConfig.class)
public class PostApiControllerIntegrationTest {

    @TestConfiguration
    static class MockServiceTestConfig {
        @Bean public MemberService memberService() { return mock(MemberService.class); }
        @Bean public PostService postService() { return mock(PostService.class); }
        @Bean public CommentService commentService() { return mock(CommentService.class); }
        @Bean public PostImageService postImageService() { return mock(PostImageService.class); }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostImageRepository postImageRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreCategoryRepository storeCategoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private MemberService memberService;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostImageService postImageService;

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
    @DisplayName("게시글 목록 조회")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testGetAllPosts() throws Exception {
        PostResponse postResponse = PostResponse.builder()
                .id(mockPost.getId())
                .title(mockPost.getTitle())
                .content(mockPost.getContent())
                .build();

        List<PostResponse> postResponses = List.of(postResponse);

        when(postService.findByStoreId(mockStore.getId(),false)).thenReturn(postResponses);

        mockMvc.perform(get("/api/post/list/{storeId}", mockStore.getId()))
                .andExpect(status().isOk())
                .andExpect(content().json("[{'id': " + mockPost.getId() + ", 'title': '" + mockPost.getTitle() + "', 'content': '" + mockPost.getContent() + "'}]"));
    }

    @DisplayName("게시글 ID로 조회")
    @Test
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testGetPostById() throws Exception {
        Long postId = mockPost.getId();

        when(postService.getPostById(postId,false)).thenReturn(PostResponse.from(mockPost));
        when(commentService.findByPostId(postId)).thenReturn(List.of(new CommentResponse(mockComment.getId(), postId, mockMember, mockComment.getContent(),LocalDateTime.now())));
        when(postImageService.findImagesByPostId(postId)).thenReturn(List.of(new PostImageResponse(mockPostImage.getId(), mockPost.getId(), mockPostImage.getS3Url())));

        mockMvc.perform(get("/api/post/{postId}", postId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
    }

    @DisplayName("게시글 생성 - 인증된 사용자")
    @Test
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testCreatePost() throws Exception {
        CreatePostRequest createPostRequest = CreatePostRequest.builder()
                .storeId(mockStore.getId())
                .title("Title 1")
                .content("Content 1")
                .build();

        MemberResponse memberResponse = new MemberResponse(mockMember);
        PostResponse createdPostResponseDto = PostResponse.builder()
                .id(1L)
                .storeId(mockStore.getId())
                .member(memberResponse)
                .title("Title 1")
                .content("Content 1")
                .thumbnail("Thumbnail")
                .build();

        when(postService.createPost(any(MemberInfo.class), any(CreatePostRequest.class), any(MultipartFile[].class)))
                .thenReturn(createdPostResponseDto);

        MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image content 1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "image content 2".getBytes());

        mockMvc.perform(multipart("/api/post/create")
                        .file(image1)
                        .file(image2)
                        .param("storeId", String.valueOf(mockStore.getId()))
                        .param("title", "Title 1")
                        .param("content", "Content 1")
                        .param("thumbnail", "Thumbnail"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.storeId").value(mockStore.getId()))
                .andExpect(jsonPath("$.title").value("Title 1"))
                .andExpect(jsonPath("$.content").value("Content 1"))
                .andExpect(jsonPath("$.thumbnail").value("Thumbnail"));
    }

    @DisplayName("게시글 생성 - 이미지 부족")
    @Test
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testCreatePost_InsufficientImages() throws Exception {
        MockMultipartFile image = new MockMultipartFile("images", "image.jpg", "image/jpeg", "image content".getBytes());

        mockMvc.perform(multipart("/api/post/create")
                        .file(image)
                        .param("storeId", String.valueOf(mockStore.getId()))
                        .param("title", "Title 1")
                        .param("content", "Content 1")
                        .param("thumbnail", "Thumbnail"))
                .andExpect(status().isBadRequest())
                .andExpect(result -> assertTrue(result.getResolvedException() instanceof ImageCountException));
    }

    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    @DisplayName("게시글 삭제")
    @Test
    void testDeletePost() throws Exception {
        Long postId = mockPost.getId();

        doNothing().when(postService).deletePost(postId);

        mockMvc.perform(delete("/api/post/{id}", postId))
                .andExpect(status().isNoContent());
    }

    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("게시글 수정")
    @Test
    void testUpdatePost() throws Exception {
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
                .thenReturn(updatedPost);

        MockMultipartFile images = new MockMultipartFile(
                "images",
                "test-image.jpg",
                "image/jpeg",
                new byte[]{}
        );

        mockMvc.perform(multipart("/api/post/{postId}", postId)
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

    @DisplayName("게시글 비활성화 - 인증된 사용자")
    @Test
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void testDisablePost() throws Exception {
        Long postId = mockPost.getId();

        doNothing().when(postService).disablePost(postId);

        mockMvc.perform(patch("/api/post/{postId}/disable", postId))
                .andExpect(status().isNoContent());
    }

    @DisplayName("게시글 비활성화 - 관리자")
    @Test
    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    void testDisablePostByAdmin() throws Exception {
        Long postId = mockPost.getId();

        doNothing().when(postService).disablePost(postId);

        mockMvc.perform(patch("/api/post/{postId}/disable", postId))
                .andExpect(status().isNoContent());
    }

    @DisplayName("게시글 비활성화 - 인증되지 않은 사용자")
    @Test
    void testDisablePost_Unauthenticated() throws Exception {
        Long postId = mockPost.getId();

        mockMvc.perform(patch("/api/post/{postId}/disable", postId))
                .andExpect(status().isUnauthorized()); // 401 expected
    }

}

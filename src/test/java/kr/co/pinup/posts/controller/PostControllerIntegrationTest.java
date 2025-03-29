package kr.co.pinup.posts.controller;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.repository.PostRepository;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class PostControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;

    private Member mockMember;
    private Member mockAdminMember;
    private Post mockPost;
    private Comment mockComment;
    private PostImage mockPostImage;
    private Store mockStore;

    @BeforeEach
    public void setUp() {
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
    @DisplayName("게시물 리스트 페이지 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void listPage_whenAuthenticatedUser_thenReturnsPostListView() throws Exception {
        // Given
        Long storeId = mockStore.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/list/{storeId}", storeId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/list"))
                .andExpect(model().attributeExists("posts"));
    }

    @Test
    @DisplayName("게시물 상세 페이지 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void detailPage_whenAuthenticatedUser_thenReturnsPostDetailView() throws Exception {
        // Given
        Long postId = mockPost.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/detail"))
                .andExpect(model().attributeExists("post", "comments", "images"));
    }

    @Test
    @DisplayName("게시물 생성 페이지 접근 - 권한 없는 사용자")
    void createPage_whenUnauthenticatedUser_thenRedirects() throws Exception {
        // Given
        Long storeId = mockStore.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/create").param("storeId", storeId.toString()));

        // Then
        result.andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("게시물 생성 페이지 접근 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createPage_whenAuthenticatedUser_thenReturnsCreateView() throws Exception {
        // Given
        Long storeId = mockStore.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/create").param("storeId", storeId.toString()));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/create"))
                .andExpect(model().attribute("storeId", storeId));
    }

    @Test
    @DisplayName("게시물 생성 페이지 접근 - ADMIN 사용자")
    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    void createPage_whenAdminUser_thenReturnsCreateView() throws Exception {
        // Given
        Long storeId = mockStore.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/create").param("storeId", storeId.toString()));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/create"))
                .andExpect(model().attribute("storeId", storeId));
    }

    @Test
    @DisplayName("게시물 수정 페이지 접근 - 인증된 사용자")
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void updatePage_whenAuthenticatedUser_thenReturnsUpdateView() throws Exception {
        // Given
        Long postId = mockPost.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/update/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/update"))
                .andExpect(model().attributeExists("post", "images"));
    }

    @Test
    @DisplayName("게시물 수정 페이지 접근 - ADMIN 사용자")
    @WithMockMember(nickname = "행복한 관리자", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
    void updatePage_whenAdminUser_thenReturnsUpdateView() throws Exception {
        // Given
        Long postId = mockPost.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/update/{postId}", postId));

        // Then
        result.andExpect(status().isOk())
                .andExpect(view().name("views/posts/update"))
                .andExpect(model().attributeExists("post", "images"));
    }

    @Test
    @DisplayName("게시물 수정 페이지 접근 - 권한 없는 사용자")
    void updatePage_whenUnauthenticatedUser_thenRedirects() throws Exception {
        // Given
        Long postId = mockPost.getId();

        // When
        ResultActions result = mockMvc.perform(get("/post/update/{postId}", postId));

        // Then
        result.andExpect(status().is3xxRedirection());
    }
}

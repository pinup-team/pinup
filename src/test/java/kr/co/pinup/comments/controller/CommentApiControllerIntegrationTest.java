package kr.co.pinup.comments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(kr.co.pinup.comments.controller.CommentApiControllerIntegrationTest.TestMockConfig.class)
class CommentApiControllerIntegrationTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        public CommentService commentService() {
            return mock(CommentService.class);
        }

        @Bean
        public MemberService memberService() {
            return mock(MemberService.class);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private PostRepository postRepository;

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
    private CommentService commentService;

    private Member mockMember;
    private Post mockPost;
    private Comment mockComment;

    @BeforeEach
    void setUp() {
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(false);
        when(memberService.refreshAccessToken(any())).thenReturn("mocked-token");

        StoreCategory category = new StoreCategory("Category Name");
        StoreCategory savedCategory = storeCategoryRepository.save(category);

        Location location = new Location("Test Location", "12345", "Test State", "Test District", 37.7749, -122.4194, "1234 Test St.", "Suite 101");
        Location savedLocation = locationRepository.save(location);

        Store store = Store.builder()
                .name("Test Store")
                .description("Description of the store")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .storeStatus(StoreStatus.RESOLVED)
                .category(savedCategory)
                .location(savedLocation)
                .build();
        Store savedStore = storeRepository.save(store);

        mockMember = Member.builder()
                .email("test@naver.com")
                .name("test")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("providerId")
                .role(MemberRole.ROLE_USER)
                .build();
        memberRepository.save(mockMember);

        mockPost = Post.builder()
                .title("Test Title")
                .content("Test Content")
                .member(mockMember)
                .store(savedStore)
                .build();
        postRepository.save(mockPost);

        mockComment = Comment.builder()
                .post(mockPost)
                .content("댓글 내용")
                .member(mockMember)
                .build();
        commentRepository.save(mockComment);
    }

    @Test
    @DisplayName("댓글 생성 - 인증된 사용자")
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void createComment_whenAuthenticated_givenValidRequest_thenSuccess() throws Exception {
        // Given
        CreateCommentRequest request = new CreateCommentRequest("댓글입니다.");
        CommentResponse response = new CommentResponse(
                1L, mockPost.getId(), mockMember, request.content(), LocalDateTime.now()
        );
        when(commentService.createComment(any(), anyLong(), any())).thenReturn(response);

        // When
        ResultActions result = mockMvc.perform(post("/api/comment/" + mockPost.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(new ObjectMapper().writeValueAsString(request)));

        // Then
        result.andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.postId").value(mockPost.getId()))
                .andExpect(jsonPath("$.content").value("댓글입니다."));
    }

    @Test
    @DisplayName("댓글 삭제 - 인증된 사용자")
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void deleteComment_whenAuthenticated_givenExistingComment_thenNoContent() throws Exception {
        // Given
        doNothing().when(commentService).deleteComment(mockComment.getId());

        // When
        ResultActions result = mockMvc.perform(delete("/api/comment/" + mockComment.getId()));

        // Then
        result.andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("댓글 삭제 - 인증되지 않은 사용자")
    void deleteComment_whenUnauthenticated_givenExistingComment_thenUnauthorized() throws Exception {
        // When
        ResultActions result = mockMvc.perform(delete("/api/comment/" + mockComment.getId()));

        // Then
        result.andExpect(status().isUnauthorized());
    }
}
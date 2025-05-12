package kr.co.pinup.comments;

import kr.co.pinup.comments.repository.CommentRepository;
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
import kr.co.pinup.store_categories.StoreCategory;
import kr.co.pinup.store_categories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(CommentIntegrationTest.TestMockConfig.class)
public class CommentIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private CommentRepository commentRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;

    private Member member;
    private Post post;

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        public MemberService memberService() {
            MemberService mock = mock(MemberService.class);
            when(mock.isAccessTokenExpired(any(), any())).thenReturn(false);
            when(mock.refreshAccessToken(any())).thenReturn("mocked-token");
            return mock;
        }
    }

    @BeforeEach
    void setUp() {
        commentRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();

        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리"));
        Location location = locationRepository.save(new Location("서울", "12345", "서울시", "강남구", 37.5, 127.0, "주소", "상세주소"));

        Store store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .status(Status.RESOLVED)
                .imageUrl("store.jpg")
                .category(category)
                .location(location)
                .build());

        member = memberRepository.save(Member.builder()
                .email("user@test.com")
                .name("유저")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver123")
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .store(store)
                .member(member)
                .build());
    }

    @Nested
    @DisplayName("댓글 작성 흐름")
    class CreateComment {

        @Test
        @DisplayName("정상적으로 댓글이 생성된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void createComment_thenSuccess() throws Exception {
            // Given
            String requestJson = """
                {
                    "content": "첫 번째 댓글입니다."
                }
            """;

            // When & Then
            mockMvc.perform(post("/api/comment/" + post.getId())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.content").value("첫 번째 댓글입니다."))
                    .andExpect(jsonPath("$.postId").value(post.getId()));
        }
    }

    @Nested
    @DisplayName("댓글 삭제 흐름")
    class DeleteComment {

        @Test
        @DisplayName("댓글이 정상적으로 삭제된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void deleteComment_thenSuccess() throws Exception {
            // Given
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .member(member)
                    .content("삭제할 댓글")
                    .build());

            // When & Then
            mockMvc.perform(delete("/api/comment/" + comment.getId()))
                    .andExpect(status().isNoContent());

            assertThat(commentRepository.existsById(comment.getId())).isFalse();
        }

        @Test
        @DisplayName("비회원은 댓글 삭제 시 401 반환")
        void deleteComment_whenUnauthenticated_thenUnauthorized() throws Exception {
            // Given
            Comment comment = commentRepository.save(Comment.builder()
                    .post(post)
                    .member(member)
                    .content("인증 없는 삭제 요청")
                    .build());

            // When & Then
            mockMvc.perform(delete("/api/comment/" + comment.getId()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("댓글 예외 흐름")
    class CommentException {

        @Test
        @DisplayName("존재하지 않는 게시글에 댓글 작성 시 404")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void createComment_whenPostNotFound_thenFail() throws Exception {
            // Given
            String requestJson = """
                {
                    "content": "잘못된 게시글에 대한 댓글"
                }
            """;

            // When & Then
            mockMvc.perform(post("/api/comment/9999")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestJson))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("댓글이 없는데 삭제 요청 시 404")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void deleteComment_whenCommentNotFound_thenFail() throws Exception {
            mockMvc.perform(delete("/api/comment/9999"))
                    .andExpect(status().isNotFound());
        }
    }
}

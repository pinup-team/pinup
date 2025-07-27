package kr.co.pinup.postLikes;

import jakarta.transaction.Transactional;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.repository.PostLikeRepository;
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
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({PostLikeIntegrationTest.TestMockConfig.class})
class PostLikeIntegrationTest {

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

    @Autowired private MockMvc mockMvc;
    @Autowired private PostRepository postRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private PostLikeRepository postLikeRepository;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("테스트카테고리"));
        Location location = locationRepository.save(new Location("서울", "12345", "서울시", "강남구", 37.5, 127.0, "주소", "상세주소"));

        Store store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("스토어 설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(7))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());

        member = memberRepository.save(Member.builder()
                .email("like@test.com")
                .name("좋아요 사용자")
                .nickname("like_user")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver-123")
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("좋아요 테스트")
                .content("테스트 내용")
                .store(store)
                .member(member)
                .build());
    }

    @Test
    @DisplayName("로그인 유저 - 좋아요 요청 시 성공적으로 등록됨")
    @WithMockMember(nickname = "like_user", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void toggleLike_whenLoggedIn_thenSuccess() throws Exception {
        mockMvc.perform(post("/api/post-like/" + post.getId()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByCurrentUser").value(true));

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("비로그인 유저 - 좋아요 요청 시 401 에러")
    void toggleLike_whenAnonymous_thenUnauthorized() throws Exception {
        mockMvc.perform(post("/api/post-like/" + post.getId() ).with(csrf()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("좋아요가 이미 눌려있을 때 다시 요청하면 좋아요가 취소된다")
    @WithMockMember(nickname = "like_user", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void toggleLike_whenAlreadyLiked_thenCancelLike() throws Exception {
        mockMvc.perform(post("/api/post-like/" + post.getId() ).with(csrf()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/post-like/" + post.getId() ).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByCurrentUser").value(false));

        Post updated = postRepository.findById(post.getId()).orElseThrow();
        assertThat(updated.getLikeCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("존재하지 않는 게시글에 좋아요 시도 시 404")
    @WithMockMember(nickname = "like_user", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    void toggleLike_whenPostNotFound_then404() throws Exception {
        Long invalidId = 9999L;
        mockMvc.perform(post("/api/post-like/" + invalidId ).with(csrf()))
                .andExpect(status().isNotFound());
    }
}

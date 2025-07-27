package kr.co.pinup.postLikes.controller;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.repository.PostLikeRepository;
import kr.co.pinup.postLikes.service.PostLikeService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test") 
@Import(PostLikeApiControllerIntegrationTest.MockServiceTestConfig.class)
class PostLikeApiControllerIntegrationTest {

    @TestConfiguration
    public static class MockServiceTestConfig {
        @Bean
        public MemberService memberService() {
            MemberService mock = mock(MemberService.class);
            when(mock.isAccessTokenExpired(any(), any())).thenReturn(false);
            when(mock.refreshAccessToken(any())).thenReturn("mocked-token");
            return mock;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PostLikeService postLikeService;
    @Autowired private PostRepository postRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private MemberService memberService;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("Category"));
        Location location = locationRepository.save(new Location(
                "Test Location", "12345", "Test State", "Test District",
                37.7749, -122.4194, "1234 Test St.", "Suite 101"));

        Store store = storeRepository.save(Store.builder()
                .name("테스트 가게")
                .category(category)
                .location(location)
                .description("테스트 설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .storeStatus(StoreStatus.RESOLVED)
                .build());

        member = memberRepository.save(Member.builder()
                .email("test@pinup.kr")
                .name("테스트 유저")
                .nickname("행복한돼지")
                .providerId("1234")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("게시글 제목")
                .content("내용")
                .member(member)
                .store(store)
                .version(0L)
                .build());
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("좋아요 API는 실제 DB에 반영되어야 한다")
    void toggleLike_shouldReflectInDatabase() throws Exception {
        mockMvc.perform(post("/api/post-like/{postId}", post.getId()))
                .andExpect(status().isOk());

        assertThat(postLikeRepository.existsByPostIdAndMemberId(post.getId(), member.getId())).isTrue();
    }
}

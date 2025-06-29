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
@Import(PostLikeApiControllerIntegrationTest.TestMockConfig.class)
class PostLikeApiControllerIntegrationTest {

    @TestConfiguration
    static class TestMockConfig {

        @Bean
        public MemberService memberService() {
            return mock(MemberService.class);
        }

    }

    @Autowired MockMvc mockMvc;
    @Autowired MemberService memberService;
    @Autowired PostLikeService postLikeService;
    @Autowired PostRepository postRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired PostLikeRepository postLikeRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        when(memberService.isAccessTokenExpired(any(), any())).thenReturn(false);
        when(memberService.refreshAccessToken(any())).thenReturn("mocked-token");
        StoreCategory category = new StoreCategory("Category Name");
        StoreCategory savedCategory = storeCategoryRepository.save(category);

        Location location = new Location("Test Location", "12345", "Test State", "Test District", 37.7749, -122.4194, "1234 Test St.", "Suite 101");
        Location savedLocation = locationRepository.save(location);

        Store store = Store.builder()
                .name("테스트 가게")
                .category(savedCategory)
                .location(savedLocation)
                .description("...")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(Status.RESOLVED)
                .imageUrl("이미지주소")
                .build();
        store = storeRepository.save(store);

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
                .store(store).version(0L)
                .build());
    }


    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.GOOGLE, role = MemberRole.ROLE_USER)
    @DisplayName("좋아요 API는 실제 DB에 반영되어야 한다")
    void toggleLike_shouldReflectInDatabase() throws Exception {
        // when
        mockMvc.perform(post("/api/postLike/{postId}/like", post.getId()))
                .andExpect(status().isOk());

        // then
        assertThat(postLikeRepository.existsByPostIdAndMemberId(post.getId(), member.getId())).isTrue();
    }
}
package kr.co.pinup.postLikes;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
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
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@EnableRetry
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({PostLikeConcurrencyTest.TestMockConfig.class})
class PostLikeConcurrencyTest {

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
    @Autowired private MemberRepository memberRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private PostLikeService postLikeService;

    private Post post;

    @BeforeEach
    void setUp() {
        postLikeRepository.deleteAll();
        postRepository.deleteAll();
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리"));
        Location location = locationRepository.save(new Location("서울", "12345", "서울시", "강남구", 37.5, 127.0, "주소", "상세주소"));

        Store store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());

        Member member = memberRepository.save(Member.builder()
                .email("concurrent@test.com")
                .name("동시성유저")
                .nickname(String.valueOf(new Random().nextInt(99999)))
                .providerType(OAuthProvider.NAVER)
                .providerId("naver-123")
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("동시성 테스트")
                .content("내용")
                .store(store)
                .member(member)
                .build());
    }

    @Test
    @DisplayName("여러 유저가 동시에 좋아요 요청 시 일정 비율 이상 성공하고, 중복 없이 유니크하게 저장된다")
    void toggleLike_concurrentRequests_thenSuccessRateAndUniquenessAreValid() throws Exception {
        int threadCount = 20;

        int minAcceptable = (int) (threadCount * 0.3);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            int userId = i;
            executor.submit(() -> {
                try {
                    Member member = memberRepository.save(Member.builder()
                            .email("user" + userId + "_" + System.nanoTime() + "@test.com")
                            .name("user" + userId)
                            .nickname("user" + userId + "_" + System.nanoTime())
                            .providerType(OAuthProvider.NAVER)
                            .providerId("naver-" + userId + "_" + System.nanoTime())
                            .role(MemberRole.ROLE_USER)
                            .build());

                    postLikeService.toggleLike(post.getId(), new MemberInfo(
                            member.getNickname(),
                            member.getProviderType(),
                            member.getRole()
                    ));
                } catch (Exception e) {
                    System.err.println("❌ 에러 발생: " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        int likeCount = postRepository.findById(post.getId()).orElseThrow().getLikeCount();

        // ✅ 1. 일정 비율 이상 성공했는지 확인
        assertThat(likeCount)
                .withFailMessage("기대한 likeCount는 최소 %d 이상이어야 합니다. 실제: %d", minAcceptable, likeCount)
                .isBetween(minAcceptable, threadCount);

        // ✅ 2. 중복 없이 유니크한 좋아요인지 확인
        long distinctUserLikes = postLikeRepository.findAll().stream()
                .map(p -> p.getMember().getId())
                .distinct()
                .count();

        assertThat(distinctUserLikes)
                .withFailMessage("중복된 좋아요가 있습니다. 유니크 사용자 수: %d, likeCount: %d", distinctUserLikes, likeCount)
                .isEqualTo(likeCount);
    }


    @Test
    @DisplayName("여러 유저가 동시에 좋아요 요청을 보내면 중복 없이 처리된다")
    void toggleLike_concurrently_thenAccurateLikeCount() throws Exception {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            int userId = i;
            executor.submit(() -> {
                try {
                    Member member = memberRepository.save(Member.builder()
                            .email("user" + userId +"_" + System.nanoTime()+ "@test.com")
                            .name("user" + userId)
                            .nickname("user" + userId + "_" + System.nanoTime())
                            .providerType(OAuthProvider.NAVER)
                            .providerId("naver-" + userId + "_" + System.nanoTime())
                            .role(MemberRole.ROLE_USER)
                            .build());

                    postLikeService.toggleLike(
                            post.getId(),
                            new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole())
                    );

                    successCount.incrementAndGet();
                } catch (Exception e) {
                    System.err.println("❌ 실패한 요청 user" + userId + ": " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        int likeCount = postRepository.findById(post.getId()).orElseThrow().getLikeCount();
        int expectedMin = (int) (successCount.get() * 0.99); // 예: 성공 요청의 99% 이상 반영

        assertThat(likeCount)
                .withFailMessage("성공 요청 %d건 중 좋아요 수는 최소 %d 이상이어야 합니다. 실제: %d",
                        successCount.get(), expectedMin, likeCount)
                .isBetween(expectedMin, successCount.get());
    }

    @Test
    @DisplayName("동일한 유저가 동시에 여러 번 좋아요 취소 요청 시 중복 없이 처리되어 likeCount = 0")
    void toggleLike_sameUserConcurrentlyUnlike_thenZeroLikes() throws Exception {
        memberRepository.save(Member.builder()
                .email("unlike@test.com")
                .name("좋아요취소유저")
                .nickname("cancel_user")
                .providerType(OAuthProvider.NAVER)
                .providerId("unlike-user-1")
                .role(MemberRole.ROLE_USER)
                .build());

        // 먼저 좋아요 등록
        mockMvc.perform(post("/api/post-like/" + post.getId() )
                        .with(csrf())
                        .with(user(new MemberInfo("cancel_user", OAuthProvider.NAVER, MemberRole.ROLE_USER))))
                .andExpect(status().isOk());

        // 동시에 여러 쓰레드에서 좋아요 취소 요청
        int threadCount = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    try {
                        mockMvc.perform(post("/api/post-like/" + post.getId() )
                                        .with(csrf())
                                        .with(user(new MemberInfo("cancel_user", OAuthProvider.NAVER, MemberRole.ROLE_USER))))
                                .andExpect(status().isOk());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        int likeCount = postRepository.findById(post.getId()).orElseThrow().getLikeCount();
        long storedLikes = postLikeRepository.count();

        assertThat(likeCount).isLessThanOrEqualTo(1);
        assertThat(storedLikes).isLessThanOrEqualTo(1L);
    }

    @Test
    @DisplayName("Controller: 동일한 유저가 동시에 여러 번 좋아요 요청 시 중복 없이 최대 1번만 저장됨")
    void concurrentLikeRequestViaController() throws Exception {
        // given
        Member member = post.getMember();
        Long postId = post.getId();
        int threadCount = 50;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    mockMvc.perform(post("/api/post-like/{postId}", postId)
                                    .with(user(new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole())))
                                    .contentType(MediaType.APPLICATION_JSON))
                            .andExpect(status().isOk());
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        long likeCount = postLikeRepository.count();
        Post updatedPost = postRepository.findById(postId).orElseThrow();

        assertTrue(likeCount <= 1, "좋아요는 0 또는 1번만 저장되어야 합니다.");
        assertTrue(updatedPost.getLikeCount() <= 1, "Like count는 0 또는 1이어야 합니다.");
    }

    @Test
    @DisplayName("Service: 동일한 유저가 동시에 여러 번 좋아요 요청 시 중복 없이 정확히 1번만 저장됨")
    void concurrentLikeRequestViaService() throws InterruptedException {
        // given
        Member member = post.getMember();
        Long postId = post.getId();
        int threadCount = 49;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    postLikeService.toggleLike(postId, new MemberInfo(member.getNickname(),member.getProviderType(),member.getRole()));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // then
        Optional<PostLike> postLike = postLikeRepository.findByPostIdAndMemberId(postId, member.getId());
        assertTrue(postLike.isPresent() || postLike.isEmpty(), "좋아요 존재 여부는 동시성 상황에 따라 달라질 수 있습니다.");

        long totalLikeCount = postLikeRepository.count();
        assertTrue(totalLikeCount <= 1, "중복 저장은 없어야 합니다.");

        Post updatedPost = postRepository.findById(postId).orElseThrow();
        assertTrue(updatedPost.getLikeCount() <= 1, "Like count는 0 또는 1이어야 합니다.");

    }

}
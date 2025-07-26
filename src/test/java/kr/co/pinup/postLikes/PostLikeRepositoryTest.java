package kr.co.pinup.postLikes;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class PostLikeRepositoryTest {

    @Autowired
    private PostLikeRepository postLikeRepository;

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

    private Member member;
    private Post post;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리"));
        Location location = locationRepository.save(new Location("위치", "12345", "서울시", "강남구",
                37.1234, 127.1234, "서울시 강남구", "101호"));
        Store store = storeRepository.save(Store.builder()
                .name("매장")
                .description("설명")
                .category(category)
                .location(location)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .storeStatus(StoreStatus.RESOLVED)
                .build());

        member = memberRepository.save(Member.builder()
                .email("test@naver.com")
                .name("테스트")
                .nickname("nickname")
                .providerType(OAuthProvider.NAVER)
                .providerId("pid")
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("제목")
                .content("내용")
                .member(member)
                .store(store)
                .build());
    }

    @Test
    @DisplayName("existsByPostIdAndMemberId - 좋아요 존재 여부 확인")
    void existsByPostIdAndMemberId_test() {
        // Given
        postLikeRepository.save(new PostLike(post, member));

        // When
        boolean exists = postLikeRepository.existsByPostIdAndMemberId(post.getId(), member.getId());

        // Then
        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("findByPostIdAndMemberId - 좋아요 조회")
    void findByPostIdAndMemberId_test() {
        // Given
        PostLike saved = postLikeRepository.save(new PostLike(post, member));

        // When
        Optional<PostLike> found = postLikeRepository.findByPostIdAndMemberId(post.getId(), member.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
    }

    @Test
    @DisplayName("deleteByPostIdAndMemberId - 좋아요 삭제")
    void deleteByPostIdAndMemberId_test() {
        // Given
        postLikeRepository.save(new PostLike(post, member));

        // When
        postLikeRepository.deleteByPostIdAndMemberId(post.getId(), member.getId());

        // Then
        boolean exists = postLikeRepository.existsByPostIdAndMemberId(post.getId(), member.getId());
        assertThat(exists).isFalse();
    }
}

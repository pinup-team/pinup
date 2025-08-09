package kr.co.pinup.postLikes;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import kr.co.pinup.locations.Location;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.posts.Post;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Transactional
public class PostLikeEntityTest {

    @PersistenceContext
    private EntityManager em;

    private Store createStore() {
        StoreCategory category = new StoreCategory("Test Category");
        em.persist(category);

        Location location = new Location("Test", "12345", "서울", "강남구",
                37.123, 127.123, "테스트 주소", "101호");
        em.persist(location);

        Store store = Store.builder()
                .name("테스트 매장")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build();

        em.persist(store);
        return store;
    }

    private Member createMember() {
        Member member = new Member("테스트유저", "test@example.com", "testNick", "",
                OAuthProvider.NAVER, "naver-123", MemberRole.ROLE_USER, false);
        em.persist(member);
        return member;
    }

    private Post createPost(Store store, Member member) {
        Post post = Post.builder()
                .title("테스트 제목")
                .content("테스트 내용")
                .store(store)
                .member(member)
                .build();
        em.persist(post);
        return post;
    }

    @Test
    @DisplayName("PostLike 생성 및 저장 성공")
    void savePostLike_success() {
        Store store = createStore();
        Member member = createMember();
        Post post = createPost(store, member);

        PostLike postLike = new PostLike(post, member);
        em.persist(postLike);
        em.flush();

        assertThat(postLike.getId()).isNotNull();
    }

    @Test
    @DisplayName("PostLike 저장 실패 - null 필드 존재")
    void savePostLike_nullFields_fail() {
        PostLike postLike = new PostLike(null, null);

        assertThatThrownBy(() -> {
            em.persist(postLike);
            em.flush();
        }).isInstanceOfAny(
                jakarta.validation.ConstraintViolationException.class,
                org.hibernate.exception.ConstraintViolationException.class
        );
    }

    @Test
    @DisplayName("연관 관계 Lazy 로딩 확인")
    void postLike_lazyLoading_test() {
        Store store = createStore();
        Member member = createMember();
        Post post = createPost(store, member);

        PostLike postLike = new PostLike(post, member);
        em.persist(postLike);
        em.flush();
        em.clear();

        PostLike found = em.find(PostLike.class, postLike.getId());

        assertThat(found.getPost().getTitle()).isEqualTo("테스트 제목");
        assertThat(found.getMember().getNickname()).isEqualTo("testNick");
    }
}

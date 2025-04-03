package kr.co.pinup.posts;

import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class PostRepositoryTest {

    @Autowired private PostRepository postRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;

    Store store;
    Member member;

    @BeforeEach
    void setUp() {

        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리 이름"));
        Location location = locationRepository.save(new Location("테스트 지역", "12345", "서울", "강남구", 37.1234, 127.5678, "서울 강남구 도산대로 123", "101호"));

        Store builtStore = Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .status(Status.RESOLVED)
                .imageUrl("url")
                .category(category)
                .location(location)
                .build();

        store = storeRepository.save(builtStore);

        member = memberRepository.save(Member.builder()
                .email("tester@sample.com")
                .name("테스터")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-id-1234")
                .role(MemberRole.ROLE_USER)
                .build()
        );

    }

    @Test
    @DisplayName("Store ID와 isDeleted 조건으로 게시글 목록을 조회한다")
    void findByStoreIdAndIsDeleted_works() {
        // given
        Post post1 = postRepository.save(Post.builder()
                .store(store)
                .member(member)
                .title("제목1")
                .content("내용1")
                .isDeleted(false)
                .build());

        postRepository.save(Post.builder()
                .store(store)
                .member(member)
                .title("제목2")
                .content("내용2")
                .isDeleted(true)
                .build());

        // when
        List<Post> result = postRepository.findByStoreIdAndIsDeleted(store.getId(), false);

        // then
        assertThat(result).hasSize(1);
        Post found = result.get(0);

        // 상세 비교
        assertThat(found.getId()).isEqualTo(post1.getId());
        assertThat(found.getTitle()).isEqualTo(post1.getTitle());
        assertThat(found.getContent()).isEqualTo(post1.getContent());
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Post ID와 isDeleted 조건으로 게시글을 조회한다")
    void findByIdAndIsDeleted_works() {
        // given
        Post post = postRepository.save(Post.builder()
                .store(store)
                .member(member)
                .title("테스트 제목")
                .content("테스트 내용")
                .isDeleted(false)
                .build());

        // when
        var found = postRepository.findByIdAndIsDeleted(post.getId(), false);

        // then
        assertThat(found).isPresent();
        assertThat(found.get().getTitle()).isEqualTo("테스트 제목");
    }
}
package kr.co.pinup.postImages;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class PostImageRepositoryTest {

    @Autowired PostImageRepository postImageRepository;
    @Autowired PostRepository postRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    @PersistenceContext
    EntityManager em;

    Member member;
    Store store;
    Post post;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리 이름"));
        Location location = locationRepository.save(new Location("테스트 지역", "12345", "서울", "강남구", 37.1234, 127.5678, "서울 강남구 도산대로 123", "101호"));

        member = memberRepository.save(Member.builder()
                .email("test@sample.com")
                .name("테스터")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-id-1234")
                .role(MemberRole.ROLE_USER)
                .build());

        store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(1))
                .status(Status.RESOLVED)
                .imageUrl("url")
                .category(category)
                .location(location)
                .build());

        post = postRepository.save(Post.builder()
                .title("제목")
                .content("내용")
                .member(member)
                .store(store)
                .build());
    }

    @Test
    @DisplayName("게시글 ID로 PostImage 전체 조회")
    void findByPostIdTest() {
        // given
        PostImage img1 = postImageRepository.save(PostImage.builder().post(post).s3Url("a.png").build());
        PostImage img2 = postImageRepository.save(PostImage.builder().post(post).s3Url("b.png").build());

        // when
        List<PostImage> result = postImageRepository.findByPostId(post.getId());

        // then
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("게시글 ID로 PostImage 전체 삭제")
    void deleteAllByPostIdTest() {
        // given
        postImageRepository.save(PostImage.builder().post(post).s3Url("a.png").build());
        postImageRepository.save(PostImage.builder().post(post).s3Url("b.png").build());

        // when
        postImageRepository.deleteAllByPostId(post.getId());
        em.flush(); em.clear();

        // then
        List<PostImage> result = postImageRepository.findByPostId(post.getId());
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("postId + s3Url 리스트 조건으로 PostImage 조회")
    void findByPostIdAndS3UrlInTest() {
        // given
        postImageRepository.save(PostImage.builder().post(post).s3Url("img1.png").build());
        postImageRepository.save(PostImage.builder().post(post).s3Url("img2.png").build());

        // when
        List<PostImage> result = postImageRepository.findByPostIdAndS3UrlIn(post.getId(), List.of("img1.png"));

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getS3Url()).isEqualTo("img1.png");
    }

    @Test
    @DisplayName("postId 기준 가장 오래된 이미지 조회")
    void findTopByPostIdOrderByIdAscTest() {
        // given
        postImageRepository.save(PostImage.builder().post(post).s3Url("a.png").build());
        postImageRepository.save(PostImage.builder().post(post).s3Url("b.png").build());

        // when
        PostImage result = postImageRepository.findTopByPostIdOrderByIdAsc(post.getId());

        // then
        assertThat(result.getS3Url()).isEqualTo("a.png");
    }
}
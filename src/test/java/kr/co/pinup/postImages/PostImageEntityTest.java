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
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
class PostImageEntityTest {

    @Autowired PostRepository postRepository;
    @Autowired PostImageRepository postImageRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    @PersistenceContext
    EntityManager em;

    Store store;
    Member member;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리 이름"));
        Location location = locationRepository.save(new Location("지역", "12345", "서울", "강남", 37.0, 127.0, "주소", "상세주소"));

        member = memberRepository.save(Member.builder()
                .email("test@sample.com")
                .nickname("테스터")
                .name("홍길동")
                .providerId("1234")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build());

        store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .status(Status.RESOLVED)
                .imageUrl("http://image")
                .category(category)
                .location(location)
                .build());
    }

    @Test
    @DisplayName("PostImage는 Post와 연관되어 저장되어야 한다")
    void postImage_shouldBeSavedWithPost() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();
        postRepository.save(post);

        PostImage postImage = PostImage.builder()
                .s3Url("https://image.png")
                .post(post)
                .build();

        // when
        postImageRepository.save(postImage);
        em.flush(); em.clear();

        // then
        PostImage saved = postImageRepository.findById(postImage.getId()).orElseThrow();
        assertThat(saved.getS3Url()).isEqualTo("https://image.png");
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
    }

    @Test
    @DisplayName("Post 없이 저장하면 예외가 발생한다")
    void shouldFail_whenPostIsNull() {
        // given
        PostImage postImage = PostImage.builder()
                .s3Url("https://image.png")
                .post(null)
                .build();

        // when & then
        assertThatThrownBy(() -> postImageRepository.save(postImage))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("s3Url 없이 저장하면 예외가 발생한다")
    void shouldFail_whenS3UrlIsNull() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();
        postRepository.save(post);

        PostImage postImage = PostImage.builder()
                .post(post)
                .s3Url(null)
                .build();

        // when & then
        assertThatThrownBy(() -> postImageRepository.save(postImage))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Post 저장 시 PostImage도 함께 저장된다 (Cascade)")
    void postImage_shouldBeCascadedWithPost() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .postImages(new ArrayList<>())
                .build();

        PostImage img1 = PostImage.builder().s3Url("a.png").post(post).build();
        PostImage img2 = PostImage.builder().s3Url("b.png").post(post).build();
        post.getPostImages().addAll(List.of(img1, img2));

        // when
        postRepository.save(post);
        em.flush(); em.clear();

        // then
        Post saved = postRepository.findById(post.getId()).orElseThrow();
        assertThat(saved.getPostImages()).hasSize(2);
    }

    @Test
    @DisplayName("Post 삭제 시 연결된 PostImage도 함께 삭제된다")
    void deletePost_shouldAlsoDeletePostImages() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .postImages(new ArrayList<>())
                .build();

        PostImage img1 = PostImage.builder().s3Url("a.png").post(post).build();
        post.getPostImages().add(img1);

        postRepository.save(post);
        em.flush(); em.clear();

        // when
        postRepository.deleteById(post.getId());
        em.flush(); em.clear();

        // then
        List<PostImage> images = postImageRepository.findByPostId(post.getId());
        assertThat(images).isEmpty();
    }

    @Test
    @DisplayName("PostImage의 연관된 Post는 Lazy 로딩이어야 한다")
    void shouldUseLazyLoadingForPostInPostImage() {
        // given
        Post post = postRepository.save(Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build());

        PostImage postImage = postImageRepository.save(PostImage.builder()
                .s3Url("https://image.com/sample.png")
                .post(post)
                .build());

        em.flush();
        em.clear();

        // when
        PostImage found = postImageRepository.findById(postImage.getId()).orElseThrow();

        // then
        assertThat(Hibernate.isInitialized(found.getPost())).isFalse();
    }
}

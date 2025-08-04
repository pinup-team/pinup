package kr.co.pinup.posts;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class PostEntityTest {

    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    @PersistenceContext EntityManager em;

    Member member;
    Store store;

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
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());
    }

    @Test
    @DisplayName("Post 저장 시 PostImage도 함께 저장된다 (Cascade)")
    void shouldPersistPostWithImages() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();

        PostImage img1 = PostImage.builder().s3Url("img1.png").post(post).build();
        PostImage img2 = PostImage.builder().s3Url("img2.png").post(post).build();
        post.getPostImages().addAll(new ArrayList<>(List.of(img1, img2)));

        // when
        postRepository.save(post);
        em.flush();
        em.clear();

        // then
        Post found = postRepository.findById(post.getId()).orElseThrow();
        assertThat(found.getPostImages()).hasSize(2);
    }

    @Test
    @DisplayName("Post에서 댓글 제거 시 orphanRemoval 설정에 따라 DB에서도 삭제된다")
    void orphanRemovalTest() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .comments(new ArrayList<>())
                .build();

        Comment comment = Comment.builder()
                .content("댓글")
                .post(post)
                .member(member)
                .build();

        post.getComments().add(comment);
        postRepository.save(post);
        em.flush(); em.clear();

        // when
        Post found = postRepository.findById(post.getId()).orElseThrow();
        found.getComments().clear(); // orphanRemoval
        em.flush(); em.clear();

        // then
        Post again = postRepository.findById(post.getId()).orElseThrow();
        assertThat(again.getComments()).isEmpty();
    }

    @Test
    @DisplayName("Post 삭제 시 연결된 댓글이 Cascade 설정으로 함께 삭제된다")
    void deletePost_shouldAlsoDeleteComments() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .comments(new ArrayList<>())
                .build();

        Comment comment1 = Comment.builder().content("댓글1").post(post).member(member).build();
        Comment comment2 = Comment.builder().content("댓글2").post(post).member(member).build();
        post.getComments().addAll(List.of(comment1, comment2));

        postRepository.save(post);
        em.flush(); em.clear();

        // when
        Post savedPost = postRepository.findById(post.getId()).orElseThrow();
        postRepository.delete(savedPost);
        em.flush(); em.clear();

        // then
        long count = commentRepository.countByPostId(post.getId());
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Post 삭제 시 연결된 이미지들도 함께 삭제된다")
    void deletePost_shouldAlsoDeleteImages() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .postImages(new ArrayList<>())
                .build();

        PostImage image1 = PostImage.builder().s3Url("img1.png").post(post).build();
        PostImage image2 = PostImage.builder().s3Url("img2.png").post(post).build();
        post.getPostImages().addAll(List.of(image1, image2));

        postRepository.save(post);
        em.flush(); em.clear();

        // when
        Post savedPost = postRepository.findById(post.getId()).orElseThrow();
        postRepository.delete(savedPost);
        em.flush(); em.clear();

        // then
        assertThat(postRepository.findById(post.getId())).isEmpty();
    }

    @Test
    @DisplayName("member가 없는 상태로 Post 저장 시 예외 발생")
    void shouldFailWhenMemberIsNull() {
        // given
        Post invalid = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(null)
                .build();

        // when & then
        assertThatThrownBy(() -> postRepository.save(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("store가 없는 상태로 Post 저장 시 예외 발생")
    void shouldFailWhenStoreIsNull() {
        // given
        Post invalid = Post.builder()
                .title("제목")
                .content("내용")
                .store(null)
                .member(member)
                .build();

        // when & then
        assertThatThrownBy(() -> postRepository.save(invalid))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Post 저장 시 isDeleted는 기본값 false로 설정된다")
    void isDeleted_shouldBeFalseByDefault() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();

        // when
        Post saved = postRepository.save(post);
        em.flush();
        em.clear();

        // then
        Post found = postRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("disablePost(true) 호출 시 isDeleted는 true로 설정된다")
    void disablePost_shouldSetIsDeletedTrue() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();

        postRepository.save(post);
        em.flush();
        em.clear();

        // when
        Post found = postRepository.findById(post.getId()).orElseThrow();
        found.disablePost(true);
        em.flush();
        em.clear();

        // then
        Post again = postRepository.findById(post.getId()).orElseThrow();
        assertThat(again.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("Post의 연관된 Store, Member는 Lazy 로딩이어야 한다")
    void shouldUseLazyLoadingForAssociations() {
        // given
        Post post = Post.builder()
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build();

        postRepository.save(post);
        em.flush();
        em.clear();

        // when
        Post found = postRepository.findById(post.getId()).orElseThrow();

        // then
        assertThat(Hibernate.isInitialized(found.getStore())).isFalse();
        assertThat(Hibernate.isInitialized(found.getMember())).isFalse();
    }
}
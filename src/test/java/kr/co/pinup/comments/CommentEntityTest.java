package kr.co.pinup.comments;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.posts.Post;
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

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

@DataJpaTest
class CommentEntityTest {

    @PersistenceContext
    private EntityManager em;

    @Autowired MemberRepository memberRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired PostRepository postRepository;
    @Autowired CommentRepository commentRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    Member member;
    Post post;

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

        Store store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());

        post = postRepository.save(Post.builder()
                .title("게시물 제목")
                .content("게시물 내용")
                .member(member)
                .store(store)
                .build());
    }

    @Test
    @DisplayName("Comment 저장 시 연관관계가 제대로 설정되어야 한다")
    void shouldSaveCommentWithAssociations() {
        Comment comment = Comment.builder()
                .content("댓글입니다")
                .post(post)
                .member(member)
                .build();

        Comment saved = commentRepository.save(comment);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getPost().getId()).isEqualTo(post.getId());
        assertThat(saved.getMember().getId()).isEqualTo(member.getId());
        assertThat(saved.getContent()).isEqualTo("댓글입니다");
    }

    @Test
    @DisplayName("내용 없이 저장 시 예외 발생")
    void shouldFail_whenContentIsNull() {
        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(null)
                .build();

        assertThatThrownBy(() -> commentRepository.save(comment))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("Comment의 연관된 Post, Member는 Lazy 로딩이어야 한다")
    void shouldUseLazyLoadingForAssociations() {
        Comment comment = commentRepository.save(Comment.builder()
                .content("lazy loading 확인")
                .post(post)
                .member(member)
                .build());

        em.flush();
        em.clear();

        Comment found = commentRepository.findById(comment.getId()).orElseThrow();

        assertThat(Hibernate.isInitialized(found.getPost())).isFalse();
        assertThat(Hibernate.isInitialized(found.getMember())).isFalse();
    }

}

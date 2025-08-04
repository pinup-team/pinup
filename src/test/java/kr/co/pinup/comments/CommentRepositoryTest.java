package kr.co.pinup.comments;

import kr.co.pinup.comments.model.dto.CommentResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;

@DataJpaTest
class CommentRepositoryTest {

    @Autowired CommentRepository commentRepository;
    @Autowired PostRepository postRepository;
    @Autowired MemberRepository memberRepository;
    @Autowired StoreRepository storeRepository;
    @Autowired StoreCategoryRepository storeCategoryRepository;
    @Autowired LocationRepository locationRepository;

    Post post;
    Member member;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리"));
        Location location = locationRepository.save(new Location("지역", "12345", "서울", "강남", 37.0, 127.0, "주소", "상세주소"));

        member = memberRepository.save(Member.builder()
                .email("test@email.com")
                .nickname("tester")
                .name("홍길동")
                .providerId("1234")
                .providerType(OAuthProvider.KAKAO)
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
                .title("제목")
                .content("내용")
                .store(store)
                .member(member)
                .build());
    }

    @Test
    @DisplayName("특정 게시글 ID로 댓글 목록 조회")
    void shouldFindCommentsByPostId() {
        Comment comment1 = commentRepository.save(Comment.builder()
                .content("첫 댓글")
                .post(post)
                .member(member)
                .build());

        Comment comment2 = commentRepository.save(Comment.builder()
                .content("두 번째 댓글")
                .post(post)
                .member(member)
                .build());

        List<CommentResponse> comments = commentRepository.findByPostId(post.getId());

        assertThat(comments).hasSize(2);
    }

    @Test
    @DisplayName("댓글 삭제 기능 확인")
    void shouldDeleteCommentById() {
        Comment comment = commentRepository.save(Comment.builder()
                .content("삭제될 댓글")
                .post(post)
                .member(member)
                .build());

        commentRepository.deleteById(comment.getId());

        boolean exists = commentRepository.existsById(comment.getId());
        assertThat(exists).isFalse();
    }
}

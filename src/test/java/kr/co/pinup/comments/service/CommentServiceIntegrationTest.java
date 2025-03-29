package kr.co.pinup.comments.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.exception.comment.CommentNotFoundException;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class CommentServiceIntegrationTest {

    @Autowired
    private CommentService commentService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private StoreCategoryRepository storeCategoryRepository;
    @Autowired
    private LocationRepository locationRepository;
    @Autowired
    private StoreRepository storeRepository;

    private Member savedMember;
    private Post savedPost;
    private Store savedStore;

    @BeforeEach
    void setUp() {
        StoreCategory category = new StoreCategory("Category Name");
        StoreCategory savedCategory = storeCategoryRepository.save(category);

        Location location = new Location("Test Location", "12345", "Test State", "Test District", 37.7749, -122.4194, "1234 Test St.", "Suite 101");
        Location savedLocation = locationRepository.save(location);

        Store store = Store.builder()
                .name("Test Store")
                .description("Description of the store")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .status(Status.RESOLVED)
                .imageUrl("image_url")
                .category(savedCategory)
                .location(savedLocation)
                .build();
        savedStore = storeRepository.save(store);

        savedMember = Member.builder()
                .email("test@naver.com")
                .name("행복한 돼지")
                .nickname("행복한 돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-123")
                .role(MemberRole.ROLE_USER)
                .build();
        memberRepository.save(savedMember);

        savedPost = Post.builder()
                .title("Test Post")
                .content("Post Content")
                .member(savedMember)
                .store(savedStore)
                .build();
        postRepository.save(savedPost);
    }

    @Test
    @DisplayName("댓글 생성 ")
    void testCreateComment() {
        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("댓글 내용")
                .build();

        CommentResponse response = commentService.createComment(
                new MemberInfo(savedMember.getNickname(), savedMember.getProviderType(), savedMember.getRole()),
                savedPost.getId(),
                request
        );

        assertNotNull(response);
        assertEquals(request.content(), response.content());
        assertEquals(savedPost.getId(), response.postId());
        assertEquals(savedMember.getNickname(), response.member().getNickname());
    }

    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글")
    void testCreateComment_PostNotFound() {
        Long nonExistentPostId = 999L;
        CreateCommentRequest request = CreateCommentRequest.builder()
                .content("댓글 내용")
                .build();

        assertThrows(PostNotFoundException.class, () -> commentService.createComment(
                new MemberInfo(savedMember.getNickname(), savedMember.getProviderType(), savedMember.getRole()),
                nonExistentPostId,
                request
        ));
    }

    @Test
    @DisplayName("게시글 ID로 댓글 조회")
    void testFindByPostId() {
        Comment comment1 = Comment.builder()
                .post(savedPost)
                .member(savedMember)
                .content("Test Comment 1")
                .build();

        Comment comment2 = Comment.builder()
                .post(savedPost)
                .member(savedMember)
                .content("Test Comment 2")
                .build();

        commentRepository.save(comment1);
        commentRepository.save(comment2);

        List<CommentResponse> comments = commentService.findByPostId(savedPost.getId());

        assertEquals(2, comments.size());
        assertTrue(comments.stream().anyMatch(c -> c.content().equals("Test Comment 1")));
        assertTrue(comments.stream().anyMatch(c -> c.content().equals("Test Comment 2")));
    }

    @Test
    @DisplayName("댓글 삭제")
    void testDeleteComment() {
        Comment comment = Comment.builder()
                .post(savedPost)
                .member(savedMember)
                .content("삭제할 댓글")
                .build();
        commentRepository.save(comment);

        commentService.deleteComment(comment.getId());

        assertFalse(commentRepository.existsById(comment.getId()));
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void testDeleteComment_NotFound() {
        Long invalidId = 999L;
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(invalidId));
    }
}

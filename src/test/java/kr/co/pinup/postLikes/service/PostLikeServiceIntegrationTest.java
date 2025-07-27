package kr.co.pinup.postLikes.service;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.model.dto.PostLikeResponse;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@ActiveProfiles("test")
public class PostLikeServiceIntegrationTest {

    @Autowired private PostLikeService postLikeService;

    @Autowired private PostRepository postRepository;

    @Autowired private PostLikeRepository postLikeRepository;

    @Autowired private MemberRepository memberRepository;

    @Autowired private StoreRepository storeRepository;

    @Autowired private StoreCategoryRepository storeCategoryRepository;

    @Autowired private LocationRepository locationRepository;
    @Autowired private EntityManager entityManager;

    private Member savedMember;
    private Post savedPost;

    @BeforeEach
    void setUp() {
        StoreCategory category = storeCategoryRepository.save(new StoreCategory("Category"));

        Location location = locationRepository.save(new Location("테스트 위치", "12345", "서울시", "강남구",
                37.1234, 127.1234, "서울 강남구 테헤란로", "101호"));

        Store store = Store.builder()
                .name("테스트 매장")
                .description("설명")
                .category(category)
                .location(location)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(1))
                .storeStatus(StoreStatus.RESOLVED)
                .build();
        storeRepository.save(store);

        savedMember = Member.builder()
                .email("like@test.com")
                .name("좋아요 사용자")
                .nickname("like_user")
                .providerType(OAuthProvider.NAVER)
                .providerId("provider-123")
                .role(MemberRole.ROLE_USER)
                .build();
        memberRepository.save(savedMember);

        savedPost = Post.builder()
                .title("좋아요 테스트 포스트")
                .content("내용")
                .member(savedMember)
                .store(store)
                .build();
        postRepository.save(savedPost);
    }

    @Test
    @DisplayName("좋아요 추가 - 처음 누를 때")
    void toggleLike_firstTime_shouldAddLike() {
        // given
        MemberInfo memberInfo = new MemberInfo(savedMember.getNickname(), savedMember.getProviderType(), savedMember.getRole());

        // when
        PostLikeResponse response = postLikeService.toggleLike(savedPost.getId(), memberInfo);

        // then
        assertNotNull(response);
        assertTrue(response.likedByCurrentUser());

        Post updated = postRepository.findById(savedPost.getId()).orElseThrow();
        assertEquals(1, updated.getLikeCount());
    }

    @Test
    @DisplayName("좋아요 취소 - 이미 눌렀을 경우")
    void toggleLike_alreadyLiked_shouldRemoveLike() {
        MemberInfo memberInfo = new MemberInfo(savedMember.getNickname(), savedMember.getProviderType(), savedMember.getRole());

        postLikeService.toggleLike(savedPost.getId(), memberInfo);
        postLikeService.toggleLike(savedPost.getId(), memberInfo);

        Post updated = postRepository.findById(savedPost.getId()).orElseThrow();
        assertEquals(0, updated.getLikeCount());
    }


}


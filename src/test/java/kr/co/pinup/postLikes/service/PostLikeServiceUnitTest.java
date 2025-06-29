package kr.co.pinup.postLikes.service;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.PostLike;
import kr.co.pinup.postLikes.repository.PostLikeRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.stores.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class PostLikeServiceUnitTest {

    @InjectMocks
    private PostLikeService postLikeService;

    @Mock
    private PostLikeRepository postLikeRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AppLogger appLogger;

    private Member createMockMember() {
        Member member = new Member("행복한 돼지", "test@example.com", "happyPig",
                OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);
        ReflectionTestUtils.setField(member, "id", 100L); // ID 설정 추가
        return member;
    }

    private Post createMockPost(Long postId) {
        Member member = createMockMember();
        Store store = Store.builder().name("Dummy Store").build();
        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .member(member)
                .store(store)
                .build();
        ReflectionTestUtils.setField(post, "id", postId);
        return post;
    }

    @Test
    @DisplayName("좋아요 - 처음 누를 때는 좋아요 추가")
    void toggleLike_whenNotLiked_thenAddLike() {
        // Given
        Long postId = 1L;
        Member member = createMockMember();
        Post post = createMockPost(postId);
        MemberInfo memberInfo = new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole());

        // When
        when(memberRepository.findByNickname(member.getNickname())).thenReturn(Optional.of(member));
        when(postRepository.findByIdWithOptimisticLock(postId)).thenReturn(Optional.of(post));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        var response = postLikeService.toggleLike(postId, memberInfo);

        // Then
        assertNotNull(response);
        assertTrue(response.likedByCurrentUser());
        verify(postLikeRepository).save(any(PostLike.class));
    }


    @Test
    @DisplayName("좋아요 - 이미 좋아요한 상태에서 누르면 좋아요 취소")
    void toggleLike_whenAlreadyLiked_thenRemoveLike() {
        // Given
        Long postId = 1L;
        Member member = createMockMember();
        Post post = createMockPost(postId);
        MemberInfo memberInfo = new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole());

        // When
        when(memberRepository.findByNickname(member.getNickname())).thenReturn(Optional.of(member));
        when(postRepository.findByIdWithOptimisticLock(postId)).thenReturn(Optional.of(post));
        when(postLikeRepository.findByPostIdAndMemberId(postId, member.getId()))
                .thenReturn(Optional.of(new PostLike(post, member)));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));

        var response = postLikeService.toggleLike(postId, memberInfo);

        // Then
        assertNotNull(response);
        assertFalse(response.likedByCurrentUser());
        verify(postLikeRepository).deleteByPostIdAndMemberId(postId, member.getId());
    }


    @Test
    @DisplayName("좋아요 실패 - 존재하지 않는 게시글")
    void toggleLike_whenPostNotFound_thenThrowException() {
        // Given
        Long postId = 999L;
        Member member = createMockMember();
        MemberInfo memberInfo = new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole());

        when(memberRepository.findByNickname(member.getNickname())).thenReturn(Optional.of(member));
        when(postRepository.findByIdWithOptimisticLock(postId)).thenReturn(Optional.empty()); // 변경

        // When & Then
        assertThrows(PostNotFoundException.class, () ->
                postLikeService.toggleLike(postId, memberInfo));

        verify(postRepository).findByIdWithOptimisticLock(postId); // 검증도 이걸로
        verify(postLikeRepository, never()).save(any());
    }


    @Test
    @DisplayName("좋아요 실패 - 존재하지 않는 회원")
    void toggleLike_whenMemberNotFound_thenThrowException() {
        // Given
        Long postId = 1L;
        Member member = createMockMember();
        MemberInfo memberInfo = new MemberInfo(member.getNickname(), member.getProviderType(), member.getRole());

        when(memberRepository.findByNickname(member.getNickname())).thenReturn(Optional.empty());

        // When & Then
        assertThrows(RuntimeException.class, () ->
                postLikeService.toggleLike(postId, memberInfo));

        verify(memberRepository).findByNickname(member.getNickname());
        verify(postRepository, never()).findById(any());
        verify(postLikeRepository, never()).save(any());
    }
}
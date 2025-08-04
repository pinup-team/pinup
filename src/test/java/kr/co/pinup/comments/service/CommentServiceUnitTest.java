package kr.co.pinup.comments.service;

import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.exception.comment.CommentNotFoundException;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceUnitTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AppLogger appLogger;

    @Test
    @DisplayName("게시글 ID로 댓글 조회 - 성공")
    void getCommentsByPostId_whenCommentsExist_thenReturnsCommentList() {
        // Given
        Long postId = 1L;
        Member mockMember = new Member("행복한 돼지", "test@example.com", "happyPig", "", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);

        List<CommentResponse> commentResponses = List.of(
                CommentResponse.builder()
                        .id(1L)
                        .postId(postId)
                        .member(mockMember)
                        .content("Test Comment 1")
                        .createdAt(LocalDateTime.now())
                        .build(),
                CommentResponse.builder()
                        .id(2L)
                        .postId(postId)
                        .member(mockMember)
                        .content("Test Comment 2")
                        .createdAt(LocalDateTime.now())
                        .build()
        );

        when(commentRepository.findByPostId(postId)).thenReturn(commentResponses);

        // When
        List<CommentResponse> result = commentService.findByPostId(postId);

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(commentRepository).findByPostId(postId);
    }

    @Test
    @DisplayName("댓글 삭제 - 성공")
    void deleteComment_whenExistingComment_thenSuccess() {
        // Given
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(true);

        // When
        commentService.deleteComment(commentId);

        // Then
        verify(commentRepository).existsById(commentId);
        verify(commentRepository).deleteById(commentId);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    void deleteComment_whenCommentNotExists_thenThrowsCommentNotFoundException() {
        // Given
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(false);

        // When & Then
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository).existsById(commentId);
        verify(commentRepository, never()).deleteById(commentId);
    }

    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @Test
    @DisplayName("댓글 생성 - 성공")
    void createComment_whenValidRequest_givenExistingPost_thenSuccess() {
        // Given
        Long postId = 1L;
        String commentContent = "Test Comment";
        Member mockMember = new Member("행복한 돼지", "test@example.com", "happyPig", "", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);

        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content(commentContent)
                .build();

        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .build();

        Comment savedComment = Comment.builder()
                .post(post)
                .member(mockMember)
                .content(commentContent)
                .build();
        ReflectionTestUtils.setField(savedComment, "id", 1L);

        CommentResponse expectedResponse = CommentResponse.builder()
                .id(savedComment.getId())
                .postId(savedComment.getPost().getId())
                .member(savedComment.getMember())
                .content(savedComment.getContent())
                .createdAt(savedComment.getCreatedAt())
                .build();

        when(memberRepository.findByNickname(mockMember.getNickname())).thenReturn(Optional.of(mockMember));
        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        // When
        CommentResponse result = commentService.createComment(
                new MemberInfo(mockMember.getNickname(), mockMember.getProviderType(), mockMember.getRole()),
                postId,
                createCommentRequest
        );

        // Then
        assertNotNull(result);
        assertEquals(expectedResponse.postId(), result.postId());
        assertEquals(expectedResponse.member(), result.member());
        assertEquals(expectedResponse.content(), result.content());
        assertEquals(expectedResponse.createdAt(), result.createdAt());

        verify(postRepository).findById(postId);
        verify(commentRepository).save(any(Comment.class));
    }

    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @Test
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글")
    void createComment_whenPostNotExists_givenValidRequest_thenThrowsPostNotFoundException() {
        // Given
        Long postId = 1L;
        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content("Test Comment")
                .build();

        Member mockMember = new Member("행복한 돼지", "test@example.com", "happyPig", "", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);

        when(memberRepository.findByNickname(mockMember.getNickname())).thenReturn(Optional.of(mockMember));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(PostNotFoundException.class, () -> commentService.createComment(
                new MemberInfo(mockMember.getNickname(), mockMember.getProviderType(), mockMember.getRole()),
                postId,
                createCommentRequest
        ));

        verify(memberRepository).findByNickname(mockMember.getNickname());
        verify(postRepository).findById(postId);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}

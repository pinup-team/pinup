package kr.co.pinup.comments.service;

import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.exception.comment.CommentNotFoundException;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Mock
    private MemberRepository memberRepository;

    @DisplayName("게시글 ID로 댓글 조회")
    @Test
    void testFindByPostId() {
        Long postId = 1L;
        Member mockMember = new Member( "행복한 돼지", "test@example.com", "happyPig", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);

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
        List<CommentResponse> result = commentService.findByPostId(postId);
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(commentRepository).findByPostId(postId);
    }

    @DisplayName("댓글 삭제")
    @Test
    void testDeleteComment() {
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(true);
        commentService.deleteComment(commentId);
        verify(commentRepository).existsById(commentId);
        verify(commentRepository).deleteById(commentId);
    }

    @DisplayName("댓글 삭제 실패 - 존재하지 않는 댓글")
    @Test
    void testDeleteComment_NotFound() {
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(false);
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository).existsById(commentId);
        verify(commentRepository, never()).deleteById(commentId);
    }
    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("댓글 생성")
    @Test
    void testCreateComment() {
        Long postId = 1L;
        String commentContent = "Test Comment";
        Member mockMember = new Member("행복한 돼지", "test@example.com", "happyPig", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);
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

        CommentResponse result = commentService.createComment(
                new MemberInfo(mockMember.getNickname(), mockMember.getProviderType(), mockMember.getRole()),
                postId,
                createCommentRequest
        );

        assertNotNull(result);
        assertEquals(expectedResponse.postId(), result.postId());
        assertEquals(expectedResponse.member(), result.member());
        assertEquals(expectedResponse.content(), result.content());
        assertEquals(expectedResponse.createdAt(), result.createdAt());

        verify(postRepository).findById(postId);
        verify(commentRepository).save(any(Comment.class));
    }


    @WithMockMember(nickname = "행복한 돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("댓글 생성 실패 - 존재하지 않는 게시글")
    @Test
    void testCreateComment_PostNotFound() {
        Long postId = 1L;
        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content("Test Comment")
                .build();
        Member mockMember = new Member( "행복한 돼지", "test@example.com", "happyPig", OAuthProvider.NAVER, "provider-id-123", MemberRole.ROLE_USER, false);

        when(memberRepository.findByNickname(mockMember.getNickname())).thenReturn(Optional.of(mockMember));
        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () ->
                commentService.createComment(
                        new MemberInfo(mockMember.getNickname(), mockMember.getProviderType(), mockMember.getRole()),
                        postId,
                        createCommentRequest
                ));

        verify(memberRepository).findByNickname(mockMember.getNickname());
        verify(postRepository).findById(postId);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}

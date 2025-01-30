package kr.co.pinup.posts.service;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import kr.co.pinup.comments.exception.comment.CommentNotFoundException;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.posts.Post;

import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;


import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @InjectMocks
    private CommentService commentService;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @Test
    void testFindByPostId() {
        // Given
        Long postId = 1L;
        List<CommentResponse> commentResponses = List.of(
                CommentResponse.builder()
                        .id(1L)
                        .postId(postId)
                        .userId(1L)
                        .content("Test Comment 1")
                        .createdAt(LocalDateTime.now())
                        .build(),
                CommentResponse.builder()
                        .id(2L)
                        .postId(postId)
                        .userId(1L)
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
    void testDeleteComment() {
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
    void testDeleteComment_NotFound() {
        // Given
        Long commentId = 1L;
        when(commentRepository.existsById(commentId)).thenReturn(false);
        // When / Then
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository).existsById(commentId);
        verify(commentRepository, never()).deleteById(commentId);
    }

    @Test
    void testCreateComment() {
        // Given
        Long postId = 1L;
        String commentContent = "Test Comment";
        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content(commentContent)
                .build();
        Post post = Post.builder()
                .title("Test Post")
                .content("Test Content")
                .build();
        Comment comment = Comment.builder()
                .post(post)
                .userId(1L)
                .content(commentContent)
                .build();

        Comment savedComment = Comment.builder()
                .post(post)
                .userId(1L)
                .content(commentContent)
                .build();

        CommentResponse expectedResponse = CommentResponse.builder()
                .id(1L)
                .postId(savedComment.getPost().getId())
                .userId(savedComment.getUserId())
                .content(savedComment.getContent())
                .createdAt(savedComment.getCreatedAt())
                .build();


        when(postRepository.findById(postId)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(savedComment);

        CommentResponse result = commentService.createComment(postId, createCommentRequest);

        assertNotNull(result);
        assertEquals(1L, expectedResponse.getId());
        assertEquals(expectedResponse.getPostId(), result.getPostId());
        assertEquals(expectedResponse.getUserId(), result.getUserId());
        assertEquals(expectedResponse.getContent(), result.getContent());
        assertEquals(expectedResponse.getCreatedAt(), result.getCreatedAt());

        verify(postRepository).findById(postId);
        verify(commentRepository).save(any(Comment.class));
    }


    @Test
    void testCreateComment_PostNotFound() {
        // Given
        Long postId = 1L;
        CreateCommentRequest createCommentRequest = CreateCommentRequest.builder()
                .content("Test Comment")
                .build();

        when(postRepository.findById(postId)).thenReturn(Optional.empty());

        assertThrows(PostNotFoundException.class, () -> commentService.createComment(postId, createCommentRequest));

        verify(postRepository).findById(postId);
        verify(commentRepository, never()).save(any(Comment.class));
    }
}
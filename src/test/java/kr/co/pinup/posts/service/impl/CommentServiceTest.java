package kr.co.pinup.posts.service.impl;

import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.exception.general.BadRequestException;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentService commentService;

    private Post post;
    private Comment comment;
    private CreateCommentRequest commentRequest;

    @BeforeEach
    void setUp() {
        post = Post.builder()
                .storeId(123L)
                .userId(456L)
                .title("Test Post")
                .content("This is a test post.")
                .build();

        comment = Comment.builder()
                .post(post)
                .userId(789L)
                .content("This is a test comment.")
                .build();

        CreateCommentRequest commentRequest = CreateCommentRequest.builder()
                .userId(789L)
                .content("This is a test comment.")
                .build();
    }

    @Test
    void createComment_ValidComment_CreatesComment() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.of(post));
        when(commentRepository.save(any(Comment.class))).thenReturn(comment);

        // when
        CommentResponse result = commentService.createComment(1L, commentRequest);

        // then
        assertNotNull(result);
        assertEquals("This is a test comment.", result.getContent());
        assertEquals(789L, result.getUserId());
        assertEquals(1L, result.getPostId());
        assertEquals(1L, result.getId());

        verify(postRepository).findById(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    void createComment_PostNotFound_ThrowsPostNotFoundException() {
        // given
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(PostNotFoundException.class, () -> commentService.createComment(1L, commentRequest));
    }

    @Test
    void createComment_EmptyContent_ThrowsBadRequestException() {
        // given
        CreateCommentRequest commentRequest = CreateCommentRequest.builder()
                .userId(789L)
                .content("")
                .build();

        when(postRepository.findById(1L)).thenReturn(Optional.of(post));

        // when & then
        assertThrows(BadRequestException.class, () -> commentService.createComment(1L, commentRequest));
    }
}

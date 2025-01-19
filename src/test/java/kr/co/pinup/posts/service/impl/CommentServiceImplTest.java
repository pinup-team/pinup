package kr.co.pinup.posts.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import kr.co.pinup.posts.exception.comment.CommentNotFoundException;
import kr.co.pinup.posts.exception.general.BadRequestException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.model.entity.CommentEntity;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.repository.CommentRepository;
import kr.co.pinup.posts.model.repository.PostRepository;
import kr.co.pinup.posts.service.imp.CommentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
public class CommentServiceImplTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PostRepository postRepository;

    @InjectMocks
    private CommentServiceImpl commentService;

    private CommentEntity commentEntity;
    private PostEntity postEntity;

    @BeforeEach
    void setUp() {
        postEntity = PostEntity.builder()
                .id(1L)
                .storeId(123L)
                .userId(456L)
                .title("Test Post")
                .content("This is a test post.")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        commentEntity = CommentEntity.builder()
                .id(1L)
                .post(postEntity)
                .userId(789L)
                .content("This is a test comment.")
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void findByPostId_PostExists_ReturnsComments() {
        when(commentRepository.findByPostId(1L)).thenReturn(List.of(commentEntity));
        List<CommentEntity> result = commentService.findByPostId(1L);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("This is a test comment.", result.get(0).getContent());
    }

    @Test
    void deleteComment_CommentExists_DeletesComment() {
        when(commentRepository.existsById(1L)).thenReturn(true);
        commentService.deleteComment(1L);
        verify(commentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteComment_CommentNotFound_ThrowsCommentNotFoundException() {
        when(commentRepository.existsById(1L)).thenReturn(false);
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(1L));
    }

    @Test
    void createComment_ValidComment_CreatesComment() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

        CommentDto commentDto = new CommentDto(1L, 789L, "This is a test comment.");
        CommentDto result = commentService.createComment(commentDto);

        assertNotNull(result);
        assertEquals("This is a test comment.", result.getContent());
        assertEquals(789L, result.getUserId());
        assertEquals(Long.valueOf(1L), result.getPostId());

        verify(postRepository).findById(1L);
        verify(commentRepository).save(any(CommentEntity.class));
    }

    @Test
    void createComment_PostNotFound_ThrowsPostNotFoundException() {
        when(postRepository.findById(1L)).thenReturn(Optional.empty());
        CommentDto commentDto = new CommentDto(1L, 789L, "This is a test comment.");
        assertThrows(PostNotFoundException.class, () -> commentService.createComment(commentDto));
    }

    @Test
    void createComment_EmptyContent_ThrowsBadRequestException() {
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));
        CommentDto commentDto = new CommentDto(1L, 789L, "");
        assertThrows(BadRequestException.class, () -> commentService.createComment(commentDto));
    }
}

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

@ExtendWith(MockitoExtension.class)  // Mockito 확장을 사용하여 mock 객체를 초기화
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
        // Test 데이터 준비
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
        // given: PostEntity와 관련된 댓글을 반환하도록 mock 설정
        when(commentRepository.findByPostId(1L)).thenReturn(List.of(commentEntity));

        // when: findByPostId 메서드 호출
        List<CommentEntity> result = commentService.findByPostId(1L);

        // then: 결과 검증
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("This is a test comment.", result.get(0).getContent());
    }

    @Test
    void deleteComment_CommentExists_DeletesComment() {
        // given: 댓글이 존재함을 mock 설정
        when(commentRepository.existsById(1L)).thenReturn(true);

        // when: deleteComment 메서드 호출
        commentService.deleteComment(1L);

        // then: deleteById가 호출되었는지 검증
        verify(commentRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteComment_CommentNotFound_ThrowsCommentNotFoundException() {
        // given: 댓글이 존재하지 않음을 mock 설정
        when(commentRepository.existsById(1L)).thenReturn(false);

        // when & then: deleteComment 메서드 호출 시 예외가 발생하는지 검증
        assertThrows(CommentNotFoundException.class, () -> commentService.deleteComment(1L));
    }

    @Test
    void createComment_ValidComment_CreatesComment() {
        // given: Post가 존재함을 mock 설정
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));
        when(commentRepository.save(any(CommentEntity.class))).thenReturn(commentEntity);

        // when: createComment 메서드 호출
        CommentDto commentDto = new CommentDto(1L, 789L, "This is a test comment.");
        CommentEntity result = commentService.createComment(commentDto);

        // then: 댓글이 생성되었는지 검증
        assertNotNull(result);
        assertEquals("This is a test comment.", result.getContent());
        assertEquals(789L, result.getUserId());
        assertEquals(postEntity, result.getPost());
    }

    @Test
    void createComment_PostNotFound_ThrowsPostNotFoundException() {
        // given: 게시글이 존재하지 않음을 mock 설정
        when(postRepository.findById(1L)).thenReturn(Optional.empty());

        // when & then: createComment 메서드 호출 시 예외가 발생하는지 검증
        CommentDto commentDto = new CommentDto(1L, 789L, "This is a test comment.");
        assertThrows(PostNotFoundException.class, () -> commentService.createComment(commentDto));
    }

    @Test
    void createComment_EmptyContent_ThrowsBadRequestException() {
        // given: 게시글이 존재함을 mock 설정
        when(postRepository.findById(1L)).thenReturn(Optional.of(postEntity));

        // when & then: 빈 댓글 내용일 경우 예외가 발생하는지 검증
        CommentDto commentDto = new CommentDto(1L, 789L, "");
        assertThrows(BadRequestException.class, () -> commentService.createComment(commentDto));
    }
}
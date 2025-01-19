package kr.co.pinup.posts.service.imp;

import jakarta.transaction.Transactional;
import kr.co.pinup.posts.exception.comment.CommentNotFoundException;
import kr.co.pinup.posts.exception.general.BadRequestException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.model.entity.CommentEntity;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.repository.CommentRepository;
import kr.co.pinup.posts.model.repository.PostRepository;
import kr.co.pinup.posts.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    public List<CommentEntity> findByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }

    @Override
    public void deleteComment(Long commentId) {
        // 댓글을 찾을 수 없는 경우 예외 처리
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException("댓글을 찾을 수 없습니다.");
        }
        commentRepository.deleteById(commentId);  // 댓글 삭제
    }

    @Override
    public CommentDto createComment(CommentDto commentDto) {
        // 게시글이 존재하는지 확인하고 없으면 예외 처리
        PostEntity post = postRepository.findById(commentDto.getPostId())
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        if (commentDto.getContent().isEmpty()) {
            throw new BadRequestException("댓글 내용은 비어 있을 수 없습니다.");
        }

        CommentEntity comment = CommentEntity.builder()
                .post(post)
                .userId(commentDto.getUserId())
                .content(commentDto.getContent())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();


        CommentEntity savedComment = commentRepository.save(comment);

        return new CommentDto(savedComment.getPost().getId(), savedComment.getUserId(), savedComment.getContent());

    }
}



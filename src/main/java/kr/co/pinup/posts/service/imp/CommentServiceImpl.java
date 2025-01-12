package kr.co.pinup.posts.service.imp;

import jakarta.transaction.Transactional;
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
        commentRepository.deleteById(commentId);
    }

    @Override
    public CommentEntity createComment(CommentDto commentDto) {
        // 게시글이 존재하는지 확인
        PostEntity post = postRepository.findById(commentDto.getPostId())
                .orElseThrow(() -> new RuntimeException("게시글을 찾을 수 없습니다."));

        // 댓글 생성
        CommentEntity comment = CommentEntity.builder()
                .post(post)
                .userId(commentDto.getUserId())
                .content(commentDto.getContent())
                .createdAt(Instant.now())
                .build();

        return commentRepository.save(comment);
    }
}

package kr.co.pinup.posts.model.repository;

import kr.co.pinup.posts.model.entity.CommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<CommentEntity, Long> {
    // 게시글 ID로 댓글 조회
    List<CommentEntity> findByPostId(Long postId);

    // 댓글 삭제
    void deleteById(Long id);
}

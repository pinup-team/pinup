package kr.co.pinup.comments.repository;

import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.model.dto.CommentResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<CommentResponse> findByPostId(Long postId);

    void deleteById(Long id);

    int countByPostId(Long id);
}

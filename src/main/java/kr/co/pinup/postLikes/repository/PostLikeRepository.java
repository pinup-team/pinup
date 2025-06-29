package kr.co.pinup.postLikes.repository;

import kr.co.pinup.postLike.PostLike;
import kr.co.pinup.posts.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {
    boolean existsByPostIdAndMemberId(Long postId, Long memberId);

    Optional<PostLike> findByPostIdAndMemberId(Long postId, Long id);

    void deleteByPostIdAndMemberId(Long postId, Long id);
}

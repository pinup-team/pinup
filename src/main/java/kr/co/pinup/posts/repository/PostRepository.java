package kr.co.pinup.posts.repository;

import jakarta.persistence.LockModeType;
import kr.co.pinup.posts.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStoreIdAndIsDeleted(Long storeId, boolean isDeleted);

    Optional<Post> findByIdAndIsDeleted(Long postId, boolean isDeleted);

    @Lock(LockModeType.OPTIMISTIC)
    @Query("SELECT p FROM Post p WHERE p.id = :id")
    Optional<Post> findByIdWithOptimisticLock(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount + 1 where p.id = :postId")
    void incrementLikeCount(@Param("postId") Long postId);

    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.likeCount = p.likeCount - 1 where p.id = :postId and p.likeCount > 0")
    void decrementLikeCount(@Param("postId") Long postId);

}

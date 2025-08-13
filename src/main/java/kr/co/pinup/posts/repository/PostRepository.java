package kr.co.pinup.posts.repository;

import jakarta.persistence.LockModeType;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.PostResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

    @Query("""
            SELECT new kr.co.pinup.posts.model.dto.PostResponse(
                p.id,
                p.member.nickname,
                p.title,
                p.thumbnail,
                p.createdAt,
                COUNT(c.id),
                p.likeCount,
                CASE
                  WHEN :memberId IS NOT NULL AND
                       EXISTS (
                         SELECT 1 FROM PostLike pl
                         WHERE pl.post.id = p.id AND pl.member.id = :memberId
                       )
                  THEN TRUE ELSE FALSE
                END
            )
            FROM Post p
            LEFT JOIN Comment c ON c.post.id = p.id
            WHERE p.store.id = :storeId
              AND p.isDeleted = :isDeleted
            GROUP BY
              p.id, p.member.nickname, p.title, p.thumbnail, p.createdAt, p.likeCount
            ORDER BY p.createdAt DESC
            """)
    List<PostResponse> findPostListItems(@Param("storeId") Long storeId, @Param("isDeleted") boolean isDeleted, @Param("memberId") Long memberId);
}

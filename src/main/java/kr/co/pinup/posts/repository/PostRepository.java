package kr.co.pinup.posts.repository;

import kr.co.pinup.posts.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    List<Post> findByStoreIdAndIsDeleted(Long storeId, boolean isDeleted);

    Optional<Post> findByIdAndIsDeleted(Long postId, boolean isDeleted);
}

package kr.co.pinup.posts.model.repository;

import kr.co.pinup.posts.model.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    List<PostEntity> findByStoreId(Long storeId);

    Optional<PostEntity> findById(Long id);

}

package kr.co.pinup.posts.model.repository;

import kr.co.pinup.posts.model.entity.PostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<PostEntity, Long> {
    // 특정 행사 ID로 게시글 목록 조회
    List<PostEntity> findByStoreId(Long storeId);

    // 게시글 ID로 조회
    Optional<PostEntity> findById(Long id);

}

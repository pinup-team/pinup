package kr.co.pinup.posts.model.repository;


import kr.co.pinup.posts.model.entity.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImageEntity, Long> {

    List<PostImageEntity> findByPostId(Long postId);

    void deleteAllByPostId(Long postId);

    List<PostImageEntity> findByPostIdAndS3UrlIn(Long id, List<String> images);

    PostImageEntity findTopByPostIdOrderByIdAsc(Long postId);
}


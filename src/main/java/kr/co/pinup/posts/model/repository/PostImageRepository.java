package kr.co.pinup.posts.model.repository;

import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImageEntity, Long> {
    // 특정 게시글에 대한 이미지 목록 조회
    List<PostImageEntity> findByPostId(Long postId);

    void deleteAllByPostId(Long postId);

    List<PostImageEntity> findByPostIdAndS3UrlIn(Long id, List<String> images);

    PostImageEntity findTopByPostIdOrderByIdAsc(Long postId);
}


package kr.co.pinup.postImages.repository;


import kr.co.pinup.postImages.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostImageRepository extends JpaRepository<PostImage, Long> {

    List<PostImage> findByPostId(Long postId);

    void deleteAllByPostId(Long postId);

    List<PostImage> findByPostIdAndS3UrlIn(Long id, List<String> images);

    PostImage findTopByPostIdOrderByIdAsc(Long postId);
}


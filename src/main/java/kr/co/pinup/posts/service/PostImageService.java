package kr.co.pinup.posts.service;

import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface PostImageService {
    List<PostImageEntity> savePostImages(List<MultipartFile> images, PostEntity post);

    void deleteAllByPost(Long postId);

    void deleteSelectedImages(Long id, List<String> images);

    PostImageEntity findFirstImageByPostId(Long postId);

    List<PostImageEntity> findImagesByPostId(Long postId);
}

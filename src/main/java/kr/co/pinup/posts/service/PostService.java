package kr.co.pinup.posts.service;

import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.entity.PostEntity;

import java.util.List;

public interface PostService {

    List<PostEntity> findByStoreId(Long storeId);

    PostEntity getPostById(Long id);

    PostEntity createPost(PostDto postDto);

    void deletePost(Long id);

    PostEntity updatePost(Long id, PostDto postDto);
}

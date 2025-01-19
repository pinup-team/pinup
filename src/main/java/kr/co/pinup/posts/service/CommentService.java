package kr.co.pinup.posts.service;

import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.model.entity.CommentEntity;

import java.util.List;

public interface CommentService {
    List<CommentEntity> findByPostId(Long postId);

    void deleteComment(Long commentId);

    CommentDto createComment(CommentDto commentDto);
}

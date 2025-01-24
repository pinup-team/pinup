package kr.co.pinup.comments.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.exception.comment.CommentNotFoundException;

import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.posts.Post;

import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommentService  {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;


    public List<CommentResponse> findByPostId(Long postId) {
        return commentRepository.findByPostId(postId);
    }


    public void deleteComment(Long commentId) {
        if (!commentRepository.existsById(commentId)) {
            throw new CommentNotFoundException("댓글을 찾을 수 없습니다.");
        }
        commentRepository.deleteById(commentId);
    }


    public CommentResponse createComment(Long postId, CreateCommentRequest commentRequest) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .post(post)
                .userId(1L)
                .content(commentRequest.getContent())
                .build();

        Comment savedComment = commentRepository.save(comment);

        return CommentResponse.builder()
                .id(savedComment.getId())
                .postId(savedComment.getPost().getId())
                .userId(savedComment.getUserId())
                .content(savedComment.getContent())
                .createdAt(savedComment.getCreatedAt())
                .build();
    }
}



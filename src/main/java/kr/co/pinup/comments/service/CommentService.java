package kr.co.pinup.comments.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.Comment;
import kr.co.pinup.comments.exception.comment.CommentNotFoundException;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final AppLogger appLogger;


    public List<CommentResponse> findByPostId(Long postId) {
        log.debug("댓글 조회 호출: postId={}", postId);
        return commentRepository.findByPostId(postId);
    }


    public void deleteComment(Long commentId) {
        log.debug("댓글 삭제 요청: commentId={}", commentId);

        if (!commentRepository.existsById(commentId)) {
            appLogger.warn(new WarnLog("삭제 시도된 댓글이 존재하지 않음")
                    .setTargetId(commentId.toString())
                    .setStatus("404"));
            throw new CommentNotFoundException("댓글을 찾을 수 없습니다.");
        }

        commentRepository.deleteById(commentId);

        appLogger.info(new InfoLog("댓글 삭제 성공")
                .setTargetId(commentId.toString())
                .setStatus("204"));
    }


    public CommentResponse createComment(MemberInfo memberInfo, Long postId, CreateCommentRequest commentRequest) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다."));

        Comment comment = Comment.builder()
                .post(post)
                .member(member)
                .content(commentRequest.content())
                .build();

        Comment savedComment = commentRepository.save(comment);
        appLogger.info(new InfoLog("댓글 생성 성공")
                .setStatus("201")
                .setTargetId(savedComment.getId().toString())
                .addDetails("postId", postId.toString(), "member", member.getNickname()));

        return CommentResponse.builder()
                .id(savedComment.getId())
                .postId(savedComment.getPost().getId())
                .member(savedComment.getMember())
                .content(savedComment.getContent())
                .createdAt(savedComment.getCreatedAt())
                .build();
    }
}



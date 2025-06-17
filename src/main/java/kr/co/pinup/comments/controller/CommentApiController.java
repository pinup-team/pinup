package kr.co.pinup.comments.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comment")
@Validated
public class CommentApiController {

    private final CommentService commentService;

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable @Positive Long commentId) {
        log.debug("댓글 삭제 API 호출: commentId={}", commentId);
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @PostMapping("/{postId}")
    public ResponseEntity<CommentResponse> createComment(@LoginMember MemberInfo memberInfo, @PathVariable @Positive Long postId, @Valid @RequestBody CreateCommentRequest createCommentRequest) {
        log.debug("댓글 생성 API 호출: postId={}, nickname={}", postId, memberInfo.nickname());
        return new ResponseEntity<>(commentService.createComment(memberInfo, postId, createCommentRequest), HttpStatus.CREATED);
    }

}
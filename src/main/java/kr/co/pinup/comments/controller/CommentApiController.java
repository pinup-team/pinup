package kr.co.pinup.comments.controller;


import jakarta.validation.Valid;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.custom.loginMember.LoginMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/comment")
public class CommentApiController {

    private final CommentService commentService;

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}")
    public ResponseEntity<CommentResponse> createComment(@LoginMember MemberInfo memberInfo, @PathVariable Long postId, @Valid @RequestBody CreateCommentRequest createCommentRequest) {
        return new ResponseEntity<>(commentService.createComment(memberInfo, postId, createCommentRequest), HttpStatus.CREATED);
    }

}

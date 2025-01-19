package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.exception.comment.CommentNotFoundException;
import kr.co.pinup.posts.exception.general.BadRequestException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.model.entity.CommentEntity;
import kr.co.pinup.posts.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("comment")
public class CommentController {

    private final CommentService commentService;

    // 댓글 삭제
    @DeleteMapping("/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long commentId) {
        try {
            commentService.deleteComment(commentId);  // 댓글 삭제
        } catch (CommentNotFoundException ex) {
            log.error("댓글 삭제 오류: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
        return ResponseEntity.ok("댓글 삭제 성공");
    }

    @ResponseBody
    @PutMapping("/{postId}")
    public ResponseEntity<?> createComment(@PathVariable Long postId, @ModelAttribute CommentDto commentDto, BindingResult bindingResult) {

        if (bindingResult.hasErrors()) {
            log.error("댓글 생성 오류: " + bindingResult.getAllErrors().get(0).getDefaultMessage());
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }

        try {
            commentDto.setUserId(1L);

            CommentDto createdComment = commentService.createComment(commentDto);

            return new ResponseEntity<>(createdComment, HttpStatus.CREATED);

        } catch (PostNotFoundException | BadRequestException ex) {
            log.error("댓글 생성 오류: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
        }
    }





}

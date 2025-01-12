package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.model.dto.CommentDto;
import kr.co.pinup.posts.service.CommentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("comment")
public class CommentController {

    private final CommentService commentService;

    // 댓글 삭제
    @DeleteMapping("/delete/{commentId}")
    public String deleteComment(@PathVariable Long commentId, @RequestParam Long postId) {
        commentService.deleteComment(commentId);
        return "redirect:/post/detail/" + postId;

    }

    // 댓글 생성
    @PutMapping("/create/{postId}")
    public String createComment(@PathVariable Long postId, @ModelAttribute CommentDto commentDto) {
        commentDto.setPostId(postId);
        commentService.createComment(commentDto);

        return "redirect:/post/detail/{postId}";
    }
}

package kr.co.pinup.posts.controller;

import jakarta.validation.constraints.Positive;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Validated
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private static final String VIEW_PATH = "views/posts";

    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;

    @GetMapping("/list/{storeId}")
    public String getAllPosts(@PathVariable @Positive Long storeId, Model model) {
        model.addAttribute("posts", postService.findByStoreIdWithCommentCount(storeId,false));
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/list";
    }

    @GetMapping("/{postId}")
    public String getPostById(@PathVariable @Positive Long postId, Model model) {
        model.addAttribute("post", postService.getPostById(postId,false));
        model.addAttribute("comments", commentService.findByPostId(postId));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));
        return VIEW_PATH + "/detail";
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @GetMapping("/create")
    public String createPostForm(@RequestParam("storeId") @Positive Long storeId, Model model) {
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/create";
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @GetMapping("/update/{postId}")
    public String updatePostForm(@PathVariable @Positive Long postId, Model model) {
        model.addAttribute("post", postService.getPostById(postId,false));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));
        return VIEW_PATH + "/update";
    }

}
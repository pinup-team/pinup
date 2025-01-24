package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;

import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;

    @GetMapping("/list/{storeid}")
    public String getAllPosts(@PathVariable Long storeid, Model model) {
        log.info("getAllPosts {}", postService.findByStoreId(storeid));
        model.addAttribute("posts", postService.findByStoreId(storeid));
        return "views/post/list";
    }

    @GetMapping("/{postId}")
    public String getPostById(@PathVariable Long postId, Model model) {

        model.addAttribute("post", postService.getPostById(postId));
        model.addAttribute("comments", commentService.findByPostId(postId));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));

        return "views/post/detail";
    }

    @GetMapping("/create")
    public String createPostForm(Model model) {
        model.addAttribute("createPostRequest", new CreatePostRequest());
        return "views/post/create";
    }

    @GetMapping("/update/{id}")
    public String updatePostForm(@PathVariable Long id, Model model) {

        model.addAttribute("post", postService.getPostById(id));
        model.addAttribute("images", postImageService.findImagesByPostId(id));

        return "views/post/update";
    }

}


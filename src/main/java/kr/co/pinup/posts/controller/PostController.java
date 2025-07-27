package kr.co.pinup.posts.controller;

import jakarta.validation.constraints.Positive;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.postLikes.service.PostLikeService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final PostLikeService postLikeService;

    @GetMapping("/list/{storeId}")
    public String getAllPosts(@PathVariable @Positive Long storeId,
                              @AuthenticationPrincipal MemberInfo memberInfo,
                              Model model) {
        log.debug("게시글 목록 뷰 진입: storeId={}, memberId={}", storeId,
                memberInfo != null ? memberInfo.getUsername() : "비로그인");

        model.addAttribute("posts", postService.findByStoreIdWithCommentsAndLikes(storeId, false, memberInfo));
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/list";
    }

    @GetMapping("/{postId}")
    public String getPostById(@PathVariable @Positive Long postId,
                              @AuthenticationPrincipal MemberInfo memberInfo,
                              Model model) {
        log.debug("게시글 상세 뷰 진입: postId={}", postId);

        model.addAttribute("post", postService.getPostById(postId, false));
        model.addAttribute("comments", commentService.findByPostId(postId));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));
        model.addAttribute("likeInfo", postLikeService.getLikeInfo(postId, memberInfo));
        return VIEW_PATH + "/detail";
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @GetMapping("/create")
    public String createPostForm(@RequestParam("storeId") @Positive Long storeId, Model model) {
        log.debug("게시글 작성 폼 진입: storeId={}", storeId);
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/create";
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @GetMapping("/update/{postId}")
    public String updatePostForm(@PathVariable @Positive Long postId, Model model) {
        log.debug("게시글 수정 폼 진입: postId={}", postId);
        model.addAttribute("post", postService.getPostById(postId,false));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));
        return VIEW_PATH + "/update";
    }

}
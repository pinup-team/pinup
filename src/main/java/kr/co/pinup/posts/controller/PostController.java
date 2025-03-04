package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {

    private static final String VIEW_PATH = "views/posts";

    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;
    private final MemberRepository memberRepository;

    @GetMapping("/list/{storeId}")
    public String getAllPosts(@AuthenticationPrincipal MemberInfo memberInfo, @PathVariable Long storeId, Model model) {
        if (memberInfo != null) {
            Member member = memberRepository.findByNickname(memberInfo.nickname())
                    .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));
            model.addAttribute("member", member);
        }
        model.addAttribute("posts", postService.findByStoreId(storeId));
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/list";
    }

    @GetMapping("/{postId}")
    public String getPostById(@AuthenticationPrincipal MemberInfo memberInfo,@PathVariable Long postId, Model model) {
        if (memberInfo != null) {
            Member member = memberRepository.findByNickname(memberInfo.nickname())
                    .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));
            model.addAttribute("member", member);
        }
        model.addAttribute("post", postService.getPostById(postId));
        model.addAttribute("comments", commentService.findByPostId(postId));
        model.addAttribute("images", postImageService.findImagesByPostId(postId));

        return VIEW_PATH + "/detail";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER')")
    @GetMapping("/create")
    public String createPostForm(@RequestParam("storeId") Long storeId, Model model) {
        model.addAttribute("storeId", storeId);
        return VIEW_PATH + "/create";
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER')")
    @GetMapping("/update/{id}")
    public String updatePostForm(@PathVariable Long id, Model model) {

        model.addAttribute("post", postService.getPostById(id));
        model.addAttribute("images", postImageService.findImagesByPostId(id));

        return VIEW_PATH + "/update";
    }

}
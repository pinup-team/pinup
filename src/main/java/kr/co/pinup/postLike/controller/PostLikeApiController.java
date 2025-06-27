package kr.co.pinup.postLike.controller;

import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.postLike.model.dto.PostLikeResponse;
import kr.co.pinup.postLike.service.PostLikeService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/postLike")
@Validated
public class PostLikeApiController {

    private final PostLikeService postLikeService;

    @PostMapping("/{postId}/like")
    @ResponseBody
    public PostLikeResponse toggleLike(@PathVariable Long postId,
                                       @AuthenticationPrincipal MemberInfo memberInfo) {
        return postLikeService.toggleLike(postId, memberInfo);
    }
}

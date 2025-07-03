package kr.co.pinup.postLikes.controller;

import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.postLikes.service.PostLikeService;
import kr.co.pinup.postLikes.model.dto.PostLikeResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
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

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @PostMapping("/{postId}/like")
    @ResponseBody
    public PostLikeResponse toggleLike(@PathVariable Long postId,
                                       @AuthenticationPrincipal MemberInfo memberInfo) {
        log.debug("게시글 좋아요API 호출: postId={}, nickname={}", postId, memberInfo.nickname());
        return postLikeService.toggleLike(postId, memberInfo);
    }
}

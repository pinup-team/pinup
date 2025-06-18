package kr.co.pinup.posts.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.postImages.exception.postimage.PostImageUpdateCountException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostDetailResponse;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;

    @GetMapping("/list/{storeId}")
    public List<PostResponse> getAllPosts(@PathVariable @Positive Long storeId) {
        log.debug("게시글 목록 API 호출: storeId={}", storeId);
        return postService.findByStoreId(storeId,false);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse getPostById(@PathVariable @Positive Long postId) {
        log.debug("게시글 단건 조회 API 호출: postId={}", postId);
        PostResponse post = postService.getPostById(postId,false);
        List<CommentResponse> comments = commentService.findByPostId(postId);
        List<PostImageResponse> images = postImageService.findImagesByPostId(postId);
        return PostDetailResponse.from(post, comments, images);
    }

    @PostMapping("/create")
    public ResponseEntity<PostResponse> createPost(@AuthenticationPrincipal MemberInfo memberInfo,
                                                   @RequestPart("post") @Valid CreatePostRequest post,
                                                   @RequestPart(name = "images") List<MultipartFile> images
    ) {
        log.info("게시글 생성 요청 수신: writer={}, title={}", memberInfo.nickname(), post.title());

        if (images == null || images.size() < 2) {
            log.warn("이미지 수 부족: images={}", images != null ? images.size() : 0);
            throw new PostImageUpdateCountException("이미지는 2장 이상 등록해야 합니다.");
        }
        CreatePostImageRequest imageRequest = new CreatePostImageRequest(images);
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(memberInfo, post, imageRequest)
        );
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_ADMIN'))")
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        log.info("게시글 삭제 요청 수신: postId={}", postId);
        postService.deletePost(postId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @PutMapping("/{postId}")
    public ResponseEntity<PostResponse> updatePost(    @PathVariable Long postId,
                                                       @RequestPart("updatePostRequest") @Valid UpdatePostRequest updatePostRequest,
                                                       @RequestParam(required = false) List<String> imagesToDelete,
                                                       @RequestPart(name = "images", required = false) MultipartFile[] images) {
        log.info("게시글 수정 요청 수신: postId={}", postId);
        return ResponseEntity.ok(postService.updatePost(postId, updatePostRequest, images, imagesToDelete));
    }

    @PreAuthorize("isAuthenticated() and (hasRole('ROLE_USER') or hasRole('ROLE_ADMIN'))")
    @PatchMapping("/{postId}/disable")
    public ResponseEntity<Void> disablePost(@PathVariable Long postId) {
        log.info("게시글 비활성화 요청 수신: postId={}", postId);
        postService.disablePost(postId);
        return ResponseEntity.noContent().build();
    }

}

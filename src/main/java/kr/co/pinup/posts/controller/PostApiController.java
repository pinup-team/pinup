package kr.co.pinup.posts.controller;

import jakarta.validation.Valid;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostApiController {

    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;

    @GetMapping("/list/{storeid}")
    public List<PostResponse> getAllPosts(@PathVariable Long storeid) {
        return postService.findByStoreId(storeid);
    }

    @GetMapping("/{postId}")
    public PostDetailResponse getPostById(@PathVariable Long postId) {
        Post post = postService.getPostById(postId);
        List<CommentResponse> comments = commentService.findByPostId(postId);
        List<PostImageResponse> images = postImageService.findImagesByPostId(postId);

        return PostDetailResponse.from(post, comments, images);
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER')")
    @PostMapping("/create")
    public ResponseEntity<PostResponse> createPost(@AuthenticationPrincipal MemberInfo memberInfo,
                                                   @ModelAttribute @Valid CreatePostRequest createPostRequest,
                                                   @RequestParam(value = "images", required = true) MultipartFile[] images) {
        return ResponseEntity.status(HttpStatus.CREATED).body(postService.createPost(memberInfo,createPostRequest, images));
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        postService.deletePost(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("isAuthenticated() and hasRole('ROLE_USER')")
    @PutMapping("/{id}")
    public ResponseEntity<Post> updatePost(@PathVariable Long id,
                                           @ModelAttribute @Valid UpdatePostRequest updatePostRequest,
                                           @RequestParam("imagesToDelete") List<String> imagesToDelete,
                                           @RequestParam("images") MultipartFile[] images) {
        Post post = postService.updatePost(id, updatePostRequest,images,imagesToDelete);
        return ResponseEntity.ok(post);
    }
}

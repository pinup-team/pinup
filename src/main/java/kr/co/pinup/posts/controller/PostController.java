package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.entity.CommentEntity;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.service.CommentService;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;

    // 게시글 리스트 조회
    @PostMapping ("/list/{storeid}")
    public String getAllPosts(@PathVariable Long storeid,Model model) {
        List<PostEntity> posts = postService.findByStoreId(storeid);
        model.addAttribute("posts", posts);
        return "post/list";
    }

    // 특정 게시글 상세 조회
    @GetMapping("/{postId}")
    public String getPostById(@PathVariable Long postId, Model model) {
        // 해당 게시글에 상세 조회
        PostEntity post = postService.getPostById(postId);

        // 해당 게시글에 대한 모든 댓글 조회
        List<CommentEntity> comments = commentService.findByPostId(postId);

        // 해당 포스트에 연결된 이미지들 조회
        List<PostImageEntity> images = postImageService.findImagesByPostId(postId);

        // 모델에 댓글과 게시글 정보를 추가
        model.addAttribute("post", post);
        model.addAttribute("comments", comments);
        model.addAttribute("images", images);

        return "/post/detail/"+post.getId();
    }

    // 게시글 생성 페이지로 이동
    @GetMapping("/create")
    public String createPostForm(Model model) {
        model.addAttribute("postDto", new PostDto());
        return "/post/create";
    }


    // 게시글 생성
    @PostMapping("/create")
    public String createPost(@ModelAttribute PostDto postDto,
                             @RequestParam("images") List<MultipartFile> images,
                             Model model) {

        postDto.setUserId(1L);
        postDto.setStoreId(1L);
        postDto.setImages(images);

        PostEntity post = postService.createPost(postDto);

        model.addAttribute("post", post);

        return "/post/"+ post.getId();
    }

    // 게시글 삭제
    @DeleteMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id) {
        PostEntity post = postService.getPostById(id);
        postService.deletePost(id);
        return "redirect:/post/list/" + post.getStoreId();
    }

    // 게시글 수정
    @PutMapping("/update/{id}")
    public String updatePost(@PathVariable Long id, @ModelAttribute PostDto postDto) {
        PostEntity post = postService.getPostById(id);
        postService.updatePost(id, postDto);
        return "/post/"+ id;
    }
}

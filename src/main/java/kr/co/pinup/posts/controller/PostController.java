package kr.co.pinup.posts.controller;

import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.PostDto;
import kr.co.pinup.posts.model.dto.PostImageDto;
import kr.co.pinup.posts.model.entity.CommentEntity;
import kr.co.pinup.posts.model.entity.PostEntity;
import kr.co.pinup.posts.model.entity.PostImageEntity;
import kr.co.pinup.posts.service.CommentService;
import kr.co.pinup.posts.service.PostImageService;
import kr.co.pinup.posts.service.PostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;

@Controller
@Log4j2
@RequiredArgsConstructor
@RequestMapping("/post")
public class PostController {
    private final PostService postService;
    private final CommentService commentService;
    private final PostImageService postImageService;


    @GetMapping ("/list/{storeid}")
    public String getAllPosts(@PathVariable Long storeid, Model model) {
        List<PostEntity> posts = postService.findByStoreId(storeid);
        model.addAttribute("posts", posts);
        return "post/list";
    }

    @GetMapping("/{postId}")
    public String getPostById(@PathVariable Long postId, Model model) {
        try {
            PostEntity post = postService.getPostById(postId);
            List<CommentEntity> comments = commentService.findByPostId(postId);
            List<PostImageEntity> images = postImageService.findImagesByPostId(postId);

            model.addAttribute("post", post);
            model.addAttribute("comments", comments);
            model.addAttribute("images", images);

            return "post/detail";
        } catch (PostNotFoundException e) {
            return "redirect:/error";
        }
    }

    @GetMapping("/create")
    public String createPostForm(Model model) {
        model.addAttribute("postDto", new PostDto());
        return "post/create";
    }

    @PostMapping("/create")
    public String createPost(@ModelAttribute PostDto postDto,
                             @RequestParam(value = "images", required = true) List<MultipartFile> images,
                             RedirectAttributes redirectAttributes) {

        if (images == null || images.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "이미지를 반드시 업로드해주세요.");
            return "redirect:/post/create";
        }

        postDto.setUserId(1L);
        postDto.setStoreId(1L);

        postDto.setPostImageDto(new PostImageDto(images));

        PostEntity post = postService.createPost(postDto);

        redirectAttributes.addFlashAttribute("post", post);

        return "redirect:/post/" + post.getId();
    }


    @DeleteMapping("/{id}")
    public String deletePost(@PathVariable Long id) {
        PostEntity post = postService.getPostById(id);
        postService.deletePost(id);
        return "redirect:/post/list/" + post.getStoreId();
    }


    @GetMapping("/update/{id}")
    public String updatePostForm(@PathVariable Long id, Model model) {
        PostEntity post = postService.getPostById(id);

        List<PostImageEntity> images = postImageService.findImagesByPostId(post.getId());

        PostDto postDto = new PostDto();
        postDto.setTitle(post.getTitle());
        postDto.setContent(post.getContent());

        PostImageDto postImageDto = new PostImageDto();
        postImageDto.setImagesToDelete(new ArrayList<>());
        postDto.setPostImageDto(postImageDto);

        model.addAttribute("postDto", postDto);
        model.addAttribute("post", post);
        model.addAttribute("postId", id);
        model.addAttribute("images", images);

        return "post/update";
    }


    @PutMapping("/{id}")
    public String updatePost(@PathVariable Long id,
                             @ModelAttribute PostDto postDto,
                             @RequestParam(value = "images", required = false) List<MultipartFile> images,
                             @RequestParam(value = "imagesToDelete", required = false) List<String> imagesToDelete,
                             RedirectAttributes redirectAttributes) {
        postDto.setStoreId(1L);  // 기본값 설정
        postDto.setUserId(1L);   // 기본값 설정

        List<MultipartFile> imageList = images != null ? images : new ArrayList<>();
        postDto.setPostImageDto(new PostImageDto(imageList, imagesToDelete));

        try {

            log.info("Update Post Request - ID: {}, PostDto: {}, Images: {}, ImagesToDelete: {}",
                    id, postDto, images, imagesToDelete);

            PostEntity post = postService.updatePost(id, postDto);
            redirectAttributes.addFlashAttribute("post", post);

            return "redirect:/post/" + post.getId();
        } catch (Exception e) {
            log.error("Error updating post: {}", e.getMessage(), e);
            throw e;
        }
    }
}


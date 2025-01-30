package kr.co.pinup.posts.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;

    @Transactional
    public PostResponse createPost(CreatePostRequest createPostRequest, MultipartFile[] images) {

        Post post = Post.builder()
                .storeId(1L)
                .userId(1L)
                .title(createPostRequest.getTitle())
                .content(createPostRequest.getContent())
                .build();

        post = postRepository.save(post);

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .build();

        List<PostImage> postImages = postImageService.savePostImages(postImageRequest, post);

        if (!postImages.isEmpty()) {
            post.updateThumbnail(postImages.get(0).getS3Url());
        }

        return PostResponse.from(post);
    }

    public List<PostResponse> findByStoreId(Long storeId) {
        List<Post> posts = postRepository.findByStoreId(storeId);
        return posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    public Post getPostById(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }

    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + postId));

        try {
            postImageService.deleteAllByPost(postId);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 중 이미지 삭제 실패. ID: " + postId);
        }

        try {
            postRepository.delete(post);
        } catch (Exception e) {
            throw new PostDeleteFailedException("게시글 삭제 실패. ID: " + postId);
        }
    }

    @Transactional
    public Post updatePost(Long id, UpdatePostRequest updatePostRequest, MultipartFile[] images, List<String> imagesToDelete) {

        Post existingPost = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .imagesToDelete(imagesToDelete != null ? imagesToDelete : new ArrayList<>())
                .build();

        boolean thumbnailUpdated = false;

            if (postImageRequest.getImagesToDelete() != null && !postImageRequest.getImagesToDelete().isEmpty()) {

                postImageService.deleteSelectedImages(id, postImageRequest);
                List<PostImageResponse> remainingImages = postImageService.findImagesByPostId(id);
                if (!remainingImages.isEmpty()) {
                    existingPost.updateThumbnail(remainingImages.get(0).getS3Url());
                    thumbnailUpdated = true;
                } else if (postImageRequest.getImages() == null || postImageRequest.getImages().isEmpty()) {
                    throw new PostImageNotFoundException("썸네일을 설정할 수 없습니다. 게시물에 남은 이미지가 없습니다.");
                }
            } else {
                log.info("삭제할 이미지가 없습니다.");
            }

            if (postImageRequest != null && postImageRequest.getImages() != null) {
                List<PostImage> postImages = postImageService.savePostImages(postImageRequest, existingPost);

                if (!postImages.isEmpty()) {
                    if (!thumbnailUpdated) {
                        existingPost.updateThumbnail(postImages.get(0).getS3Url());
                    }
                } else {
                    throw new PostImageNotFoundException("새로운 이미지를 추가했으나 썸네일을 설정할 수 없습니다.");
                }
            }

        existingPost.update(updatePostRequest.getTitle(), updatePostRequest.getContent());

        return postRepository.save(existingPost);
    }
}
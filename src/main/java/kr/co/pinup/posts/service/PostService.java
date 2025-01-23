package kr.co.pinup.posts.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    @PersistenceContext
    private EntityManager entityManager;
    private final PostRepository postRepository;
    private final PostImageService postImageService;

    @Transactional
    public Post createPost(CreatePostRequest createPostRequest, MultipartFile[] images) {

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

        return postRepository.save(post);
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

    //TODO Post 업데이트 처리시 comment 양방향으로 오류 발생 후에 처리
    @Transactional
    public Post updatePost(Long id, UpdatePostRequest updatePostRequest, MultipartFile[] images) {

        Post existingPost  = postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));

        PostImageRequest postImageRequest = PostImageRequest.builder()
                .images(Arrays.asList(images))
                .imagesToDelete(updatePostRequest.getPostImageRequest().getImagesToDelete())
                .postId(existingPost.getId())
                .build();

        if (updatePostRequest.getPostImageRequest() != null) {

            if (updatePostRequest.getPostImageRequest().getImagesToDelete() != null && !updatePostRequest.getPostImageRequest().getImagesToDelete().isEmpty()) {
                postImageService.deleteSelectedImages(id, updatePostRequest.getPostImageRequest());
            }

            if (updatePostRequest.getPostImageRequest().getImages() != null && !updatePostRequest.getPostImageRequest().getImages().isEmpty()) {

                List<PostImage> postImages = postImageService.savePostImages(postImageRequest, existingPost);

                if (!postImages.isEmpty()) {
                    PostImage firstImage = postImageService.findFirstImageByPostId(existingPost.getId());
                    if (firstImage != null) {
                        existingPost = existingPost.toBuilder()
                                .thumbnail(firstImage.getS3Url())  // 썸네일 설정
                                .build();
                    }
                }
            }
        }

//        List<Comment> clonedComments = new ArrayList<>(existingPost.getComments());
//        List<PostImage> clonedPostImages = new ArrayList<>(existingPost.getPostImages());

        existingPost = existingPost.toBuilder()
                .title(updatePostRequest.getTitle())
                .content(updatePostRequest.getContent())
//                .comments(clonedComments)
//                .postImages(clonedPostImages)
                .build();

        return postRepository.save(existingPost);
    }
}
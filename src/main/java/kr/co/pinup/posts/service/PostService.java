package kr.co.pinup.posts.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.exception.postimage.PostImageUpdateCountException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.model.dto.UpdatePostImageRequest;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostDeleteFailedException;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.exception.StoreNotFoundException;
import kr.co.pinup.stores.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostImageService postImageService;
    private final MemberRepository memberRepository;
    private final StoreRepository  storeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public PostResponse createPost(MemberInfo memberInfo, CreatePostRequest createPostRequest, CreatePostImageRequest createPostImageRequest) {
        Post post = createPostEntity(memberInfo, createPostRequest);
        post = postRepository.save(post);

        List<PostImage> postImages = postImageService.savePostImages(createPostImageRequest, post);

        if (!postImages.isEmpty()) {
            post.updateThumbnail(postImages.get(0).getS3Url());
        }

        return PostResponse.from(post);
    }

    private Post createPostEntity(MemberInfo memberInfo, CreatePostRequest createPostRequest) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."));

        Store store = storeRepository.findById(createPostRequest.storeId())
                .orElseThrow(() -> new StoreNotFoundException(createPostRequest.storeId() + "을 찾을 수 없습니다."));

        return Post.builder()
                .store(store)
                .member(member)
                .title(createPostRequest.title())
                .content(createPostRequest.content())
                .build();
    }

    public List<PostResponse> findByStoreId(Long storeId, boolean isDeleted) {
        List<Post> posts = postRepository.findByStoreIdAndIsDeleted(storeId, isDeleted);
        return posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    public List<PostResponse> findByStoreIdWithCommentCount(Long storeId, boolean isDeleted) {
        List<Post> posts = postRepository.findByStoreIdAndIsDeleted(storeId, isDeleted);
        return posts.stream()
                .map(post -> PostResponse.fromPostWithComments(post, commentRepository.countByPostId(post.getId())))
                .collect(Collectors.toList());
    }

    public PostResponse getPostById(Long id, boolean isDeleted) {
        return postRepository.findByIdAndIsDeleted(id, isDeleted)
                .map(PostResponse::from)
                .orElseThrow(PostNotFoundException::new);
    }
    
    public void deletePost(Long postId) {
        Post post = findByIdOrThrow(postId);
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
    public PostResponse  updatePost(Long id, UpdatePostRequest updatePostRequest, MultipartFile[] images, List<String> imagesToDelete) {
        Post existingPost = findByIdOrThrow(id);
        existingPost.update(updatePostRequest.title(), updatePostRequest.content());

        UpdatePostImageRequest imageRequest = buildImageUpdateRequest(images, imagesToDelete);

        if (hasImagesToDelete(imageRequest) || hasNewImagesToUpload(imageRequest)) {
            validateRemainingImageCount(id, imageRequest);
            handleImageDeletionAndUpload(id, existingPost, imageRequest);
            updateThumbnailFromCurrentImages(existingPost, id);
        }

        return PostResponse.from(postRepository.save(existingPost));
    }

    private UpdatePostImageRequest buildImageUpdateRequest(MultipartFile[] images, List<String> deleteUrls) {
        return UpdatePostImageRequest.builder()
                .images(images != null ? Arrays.asList(images) : Collections.emptyList())
                .imagesToDelete(deleteUrls != null ? deleteUrls : Collections.emptyList())
                .build();
    }

    private void validateRemainingImageCount(Long postId, UpdatePostImageRequest request) {
        int currentCount = postImageService.findImagesByPostId(postId).size();
        int deleteCount = request.getImagesToDelete().size();
        int uploadCount = (int) request.getImages().stream().filter(f -> !f.isEmpty()).count();

        int remaining = currentCount - deleteCount + uploadCount;
        if (remaining < 2) {
            throw new PostImageUpdateCountException();
        }
    }

    private void handleImageDeletionAndUpload(Long postId, Post post, UpdatePostImageRequest request) {
        if (hasImagesToDelete(request)) {
            postImageService.deleteSelectedImages(postId, request);
        }
        if (hasNewImagesToUpload(request)) {
            postImageService.savePostImages(request, post);
        }
    }

    private boolean hasImagesToDelete(UpdatePostImageRequest request) {
        return !request.getImagesToDelete().isEmpty();
    }

    private boolean hasNewImagesToUpload(UpdatePostImageRequest request) {
        return request.getImages().stream().anyMatch(file -> !file.isEmpty());
    }

    private void updateThumbnailFromCurrentImages(Post post, Long postId) {
        List<PostImageResponse> remainingImages = postImageService.findImagesByPostId(postId);
        if (!remainingImages.isEmpty()) {
            post.updateThumbnail(remainingImages.get(0).getS3Url());
        } else {
            throw new PostImageNotFoundException("썸네일을 설정할 수 없습니다.");
        }
    }

    public void disablePost(Long postId) {
        Post post = findByIdOrThrow(postId);
        post.disablePost(true);
        postRepository.save(post);
    }

    public Post findByIdOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }
}
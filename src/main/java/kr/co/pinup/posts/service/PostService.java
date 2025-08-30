package kr.co.pinup.posts.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
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
import kr.co.pinup.postLikes.repository.PostLikeRepository;
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
    private final PostLikeRepository postLikeRepository;
    private final AppLogger appLogger ;

    @Transactional
    public PostResponse createPost(MemberInfo memberInfo, CreatePostRequest createPostRequest, CreatePostImageRequest createPostImageRequest) {
        Post post = createPostEntity(memberInfo, createPostRequest);
        post = postRepository.save(post);

        appLogger.info(new InfoLog("게시글 생성 완료")
                .setStatus("201")
                .setTargetId(post.getId().toString())
                .addDetails("writer", post.getMember().getNickname(), "title", post.getTitle()));

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
        log.debug("게시글 목록 요청: storeId={}, isDeleted={}", storeId, isDeleted);
        List<Post> posts = postRepository.findByStoreIdAndIsDeleted(storeId, isDeleted);

        return posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    public List<PostResponse> findByStoreIdWithCommentsAndLikes(Long storeId, boolean isDeleted, MemberInfo memberInfo) {
        log.debug("댓글 포함 게시글 요청: storeId={}, isDeleted={}", storeId, isDeleted);

        Long memberId = (memberInfo != null) ? memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."))
                .getId() : null;

        return postRepository.findPostListItems(storeId, isDeleted, memberId);

    }

    public PostResponse getPostById(Long id, boolean isDeleted) {
        log.debug("게시글 단건 요청: postId={}, isDeleted={}", id, isDeleted);

        return postRepository.findByIdAndIsDeleted(id, isDeleted)
                .map(PostResponse::from)
                .orElseThrow(PostNotFoundException::new);
    }
    
    public void deletePost(Long postId) {
        Post post = findByIdOrThrow(postId);
        try {
            postImageService.deleteAllByPost(postId);
        } catch (Exception e) {
            appLogger.error(new ErrorLog("게시글 삭제 실패", e)
                    .setStatus("500")
                    .setTargetId(postId.toString())
                    .addDetails("reason", "이미지 삭제 실패"));
            throw new PostDeleteFailedException("게시글 삭제 중 이미지 삭제 실패. ID: " + postId);
        }
        try {
            postRepository.delete(post);
            appLogger.info(new InfoLog("게시글 삭제 성공").setStatus("200").setTargetId(postId.toString()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("게시글 삭제 실패", e)
                    .setStatus("500")
                    .setTargetId(postId.toString())
                    .addDetails("reason", e.getMessage()));
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
        appLogger.info(new InfoLog("게시글 수정 완료").setStatus("200").setTargetId(id.toString()));
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
            appLogger.warn(new WarnLog("이미지 수 부족")
                    .setStatus("400")
                    .setTargetId(postId.toString())
                    .addDetails("current", String.valueOf(currentCount), "delete", String.valueOf(deleteCount), "upload", String.valueOf(uploadCount)));
            throw new PostImageUpdateCountException();
        }
    }

    private void handleImageDeletionAndUpload(Long postId, Post post, UpdatePostImageRequest request) {
        if (hasImagesToDelete(request)) {
            postImageService.deleteSelectedImages(postId, request);
            appLogger.info(new InfoLog("이미지 삭제 및 업로드 처리").setTargetId(postId.toString()));
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

    public void updateThumbnailFromCurrentImages(Post post, Long postId) {
        try {
            List<PostImageResponse> remainingImages = postImageService.findImagesByPostId(postId);

            if (!remainingImages.isEmpty()) {
                appLogger.info(new InfoLog("썸네일 갱신 시도")
                        .setTargetId(postId.toString())
                        .addDetails("imageCount", String.valueOf(remainingImages.size())));

                post.updateThumbnail(remainingImages.get(0).getS3Url());
            } else {
                throw new PostImageNotFoundException("썸네일을 설정할 수 없습니다.");
            }

        } catch (Exception e) {
            appLogger.error(new ErrorLog("썸네일 갱신 실패", e)
                    .setStatus("404")
                    .setTargetId(postId.toString())
                    .addDetails("reason", e.getMessage()));
            throw e;
        }
    }

    public void disablePost(Long postId) {
        Post post = findByIdOrThrow(postId);
        post.disablePost(true);
        appLogger.info(new InfoLog("게시글 비활성화 처리").setStatus("200").setTargetId(postId.toString()));
        postRepository.save(post);
    }

    public Post findByIdOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }
}
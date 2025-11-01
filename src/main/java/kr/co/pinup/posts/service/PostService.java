package kr.co.pinup.posts.service;

import kr.co.pinup.cache.CacheNames;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageUpdateCountException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.UpdatePostImageRequest;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.event.PostCacheEvent;
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
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private final StoreRepository storeRepository;
    private final PostImageRepository postImageRepository;
    private final AppLogger appLogger ;
    private final ApplicationEventPublisher events;


    private record ChangeFlags(boolean hasText, boolean hasUpload, boolean hasDelete) {
    }

    private record ImageMutationResult(List<String> actuallyDeleted, boolean thumbChanged) {
    }

    @Transactional
    public PostResponse createPost(MemberInfo memberInfo, CreatePostRequest createPostRequest, CreatePostImageRequest createPostImageRequest) {

        List<String> uploadedUrls = postImageService.uploadImagesOnly(createPostImageRequest);
        postImageService.cleanupUploadedOnRollback(uploadedUrls);


        Post post = createPostEntity(memberInfo, createPostRequest);
        post = postRepository.save(post);

        List<PostImage> postImages = postImageService.saveImageUrls(post, uploadedUrls);

        if (!postImages.isEmpty()) {
            post.updateThumbnail(postImages.get(0).getS3Url());
        }
        appLogger.info(new InfoLog("게시글 생성 완료")
                .setStatus("201")
                .setTargetId(post.getId().toString())
                .addDetails("writer", post.getMember().getNickname(), "title", post.getTitle()));

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

    @Transactional(readOnly = true)
    public List<PostResponse> findByStoreId(Long storeId, boolean isDeleted) {
        log.debug("게시글 목록 요청: storeId={}, isDeleted={}", storeId, isDeleted);
        List<Post> posts = postRepository.findByStoreIdAndIsDeleted(storeId, isDeleted);

        return posts.stream()
                .map(PostResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PostResponse> findByStoreIdWithCommentsAndLikes(Long storeId, boolean isDeleted, MemberInfo memberInfo) {
        log.debug("댓글 포함 게시글 요청: storeId={}, isDeleted={}", storeId, isDeleted);

        Long memberId = (memberInfo != null) ? memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException(memberInfo.nickname() + "님을 찾을 수 없습니다."))
                .getId() : null;

        return postRepository.findPostListItems(storeId, isDeleted, memberId);

    }

    @Cacheable(
            value = CacheNames.POST_DETAIL,
            key = "#p0",
            condition = "!#p1",
            sync = true
    )
    @Transactional(readOnly = true)
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
            events.publishEvent(PostCacheEvent.deleted(postId));
            appLogger.info(new InfoLog("게시글 삭제 성공").setStatus("200").setTargetId(postId.toString()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("게시글 삭제 실패", e)
                    .setStatus("500")
                    .setTargetId(postId.toString())
                    .addDetails("reason", e.getMessage()));
            throw new PostDeleteFailedException("게시글 삭제 실패. ID: " + postId);
        }
    }

    public void disablePost(Long postId) {
        Post post = findByIdOrThrow(postId);
        post.disablePost(true);
        appLogger.info(new InfoLog("게시글 비활성화 처리").setStatus("200").setTargetId(postId.toString()));
        postRepository.save(post);
        events.publishEvent(PostCacheEvent.disabled(postId));
    }

    public Post findByIdOrThrow(Long id) {
        return postRepository.findById(id)
                .orElseThrow(() -> new PostNotFoundException("게시글을 찾을 수 없습니다. ID: " + id));
    }

    private ChangeFlags computeFlags(UpdatePostRequest req, UpdatePostImageRequest imgReq) {
        boolean hasText = (req != null) && (req.title() != null || req.content() != null);
        boolean hasUpload = hasNewImagesToUpload(imgReq);
        boolean hasDelete = hasImagesToDelete(imgReq);
        return new ChangeFlags(hasText, hasUpload, hasDelete);
    }

    private UpdatePostImageRequest buildImageUpdateRequest(MultipartFile[] images, List<String> deleteUrls) {
        return UpdatePostImageRequest.builder()
                .images(images != null ? Arrays.asList(images) : Collections.emptyList())
                .imagesToDelete(deleteUrls != null ? deleteUrls : Collections.emptyList())
                .build();
    }

    private boolean hasImagesToDelete(UpdatePostImageRequest request) {
        return !request.getImagesToDelete().isEmpty();
    }

    private boolean hasNewImagesToUpload(UpdatePostImageRequest request) {
        return request.getImages().stream().anyMatch(file -> !file.isEmpty());
    }

    @Transactional
    public PostResponse updatePost(Long id,
                                   UpdatePostRequest updatePostRequest,
                                   MultipartFile[] images,
                                   List<String> imagesToDelete) {


        UpdatePostImageRequest imgReq = buildImageUpdateRequest(images, imagesToDelete);
        ChangeFlags flags = computeFlags(updatePostRequest, imgReq);

        List<String> uploadedUrls = List.of();
        if (flags.hasUpload()) {
            // 1.1 uplode 먼저
            CreatePostImageRequest uploadReq = CreatePostImageRequest.builder()
                    .images(imgReq.getImages())
                    .build();
            uploadedUrls = postImageService.uploadImagesOnly(uploadReq);
        }


        return updatePostTx(
                id, updatePostRequest, uploadedUrls, imgReq.getImagesToDelete(),
                flags.hasText(), flags.hasUpload(), flags.hasDelete()
        );
    }


    private  PostResponse updatePostTx(Long id,
                                        UpdatePostRequest req,
                                        List<String> uploadedUrls,
                                        List<String> deleteUrls,
                                        boolean hasText, boolean hasUpload, boolean hasDelete) {
        // 2.1 load
        Post post = findByIdOrThrow(id);

        boolean imagesChanged = hasUpload || hasDelete;
        List<String> actuallyDeleted = List.of();
        boolean thumbChanged = false;

        if (imagesChanged) {
            ImageMutationResult r = mutateImagesAndRefresh(
                    post, id, hasUpload, hasDelete, uploadedUrls, deleteUrls
            );
            actuallyDeleted = r.actuallyDeleted();
            thumbChanged = r.thumbChanged();
        }

        // 2.x 텍스트 (썸네일 결정 이후)
        boolean textChanged = false;
        if (hasText) {
            textChanged = post.applyTextIfChanged(req.title(), req.content());
        }

        // 완전 무변경이면 저장 생략 (단, 실삭제 목록 있으면 afterCommit 예약)
        if (!textChanged && !thumbChanged) {
            if (!actuallyDeleted.isEmpty()) {
                postImageService.deleteS3QuietlyAfterCommit(actuallyDeleted);
            }
            if (imagesChanged ) {
                events.publishEvent(PostCacheEvent.updated(id, /*detailChanged*/ false, /*imagesChanged*/ true));            }
            return PostResponse.from(post);
        }

        // 텍스트/썸네일 둘 중 하나라도 바뀌면 save
        try {
            appLogger.info(new InfoLog("게시글 수정 완료")
                    .setStatus("200")
                    .setTargetId(id.toString()));

            return PostResponse.from(postRepository.save(post));
        } finally {
            if (!actuallyDeleted.isEmpty()) {
                postImageService.deleteS3QuietlyAfterCommit(actuallyDeleted);
            }
            events.publishEvent(PostCacheEvent.updated(
                    id,
                    /* detailChanged */  (textChanged || thumbChanged),/* imagesChanged */  (imagesChanged || thumbChanged)
            ));
        }
    }

    private ImageMutationResult mutateImagesAndRefresh(
            Post post, Long postId,
            boolean hasUpload, boolean hasDelete,
            List<String> uploadedUrls, List<String> deleteUrls) {

        if (hasUpload) {
            postImageService.cleanupUploadedOnRollback(uploadedUrls);
        }

        List<String> actuallyDeleted = List.of();
        if (hasDelete) {
            actuallyDeleted = postImageService.deleteSelectedImagesDbOnly(postId, deleteUrls);
        }

        if (hasUpload) {
            postImageService.saveImageUrls(post, uploadedUrls);
        }

        boolean thumbChanged = refreshThumbnailIfNeeded(post, postId, hasUpload || hasDelete);
        return new ImageMutationResult(actuallyDeleted, thumbChanged);
    }

    private boolean refreshThumbnailIfNeeded(Post post, Long postId, boolean imagesChanged) {
        if (!imagesChanged) return false;

        List<PostImage> remaining = postImageRepository.findAllByPostIdOrderByIdAsc(postId);
        if (remaining.size() < 2) throw new PostImageUpdateCountException();

        String current = post.getThumbnail();
        boolean stillExists = (current != null) &&
                remaining.stream().anyMatch(r -> java.util.Objects.equals(r.getS3Url(), current));

        if (!stillExists) {
            post.updateThumbnail(remaining.get(0).getS3Url());
            return true;
        }
        return false;
    }

}
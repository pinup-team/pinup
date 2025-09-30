package kr.co.pinup.postImages.service;

import kr.co.pinup.custom.s3.exception.ImageDeleteFailedException;
import kr.co.pinup.postImages.model.dto.PostImageUploadRequest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.custom.s3.S3Service;

import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.exception.postimage.PostImageSaveFailedException;
import kr.co.pinup.postImages.model.dto.CreatePostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;

import kr.co.pinup.postImages.model.dto.UpdatePostImageRequest;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostImageService  {

    private final S3Service s3Service;
    private final PostImageRepository postImageRepository;
    private final AppLogger appLogger;

    private static final String PATH_PREFIX = "post";

    public List<String> uploadImagesOnly(CreatePostImageRequest req) {
        if (req.getImages() == null || req.getImages().isEmpty()) {
            throw new PostImageNotFoundException("업로드할 이미지가 없습니다.");
        }

        List<String> imageUrls = uploadFiles(req.getImages(), PATH_PREFIX);

        appLogger.info(new InfoLog("이미지 업로드 완료")
                .setStatus("201")
                .addDetails("count", String.valueOf(imageUrls.size())));

        return imageUrls;
    }

    @Transactional
    public List<PostImage> saveImageUrls(Post post, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            throw new PostImageNotFoundException("저장할 이미지 URL이 없습니다.");
        }

        List<PostImage> postImages = imageUrls.stream()
                .map(s3Url -> new PostImage(post, s3Url))
                .collect(Collectors.toList());

        try {
            postImageRepository.saveAll(postImages);
            appLogger.info(new InfoLog("이미지 DB 저장 완료")
                    .setStatus("201")
                    .setTargetId(post.getId().toString())
                    .addDetails("count", String.valueOf(postImages.size())));
            return postImages;

        } catch (Exception e) {
            appLogger.error(new ErrorLog("이미지 DB 저장 실패", e)
                    .setStatus("500")
                    .setTargetId(post.getId().toString())
                    .addDetails("reason", e.getMessage()));
            throw new PostImageSaveFailedException("이미지 저장 중 문제가 발생했습니다.", e);
        }
    }

    public void cleanupUploadedOnRollback(List<String> uploadedUrls) {
        if (uploadedUrls == null || uploadedUrls.isEmpty()) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            appLogger.warn(new WarnLog("롤백 보상 등록 불가(트랜잭션 바깥)")
                    .addDetails("count", String.valueOf(uploadedUrls.size())));
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) deleteS3ByUrlsQuietly(uploadedUrls);
            }
        });
    }

    public void deleteS3ByUrlsQuietly(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        for (String url : imageUrls) {
            String key = PATH_PREFIX + "/" + s3Service.extractFileName(url);
            try { s3Service.deleteFromS3(key); }
            catch (Exception ex) {
                appLogger.warn(new WarnLog("보상 삭제 실패")
                        .addDetails("file", key).addDetails("reason", ex.getMessage()));
            }
        }
    }

    @Transactional
    public void deleteAllByPost(Long postId) {
        List<PostImage> postImages = postImageRepository.findByPostId(postId);
        if (postImages.isEmpty()) {
            return;
        }
        try {
            postImages.forEach(postImage -> {
                String fileUrl = postImage.getS3Url();
                String fileName = PATH_PREFIX+ "/" + s3Service.extractFileName(fileUrl);

                s3Service.deleteFromS3(fileName);
            });
            postImageRepository.deleteAllByPostId(postId);
            appLogger.info(new InfoLog("전체 이미지 삭제 완료").setTargetId(postId.toString()));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("전체 이미지 삭제 실패", e).setStatus("500").setTargetId(postId.toString()).addDetails("reason", e.getMessage()));
            throw new PostImageDeleteFailedException("이미지 삭제 중 문제가 발생했습니다.", e);
        }
    }
    @Transactional
    public void deleteSelectedImages(Long postId, UpdatePostImageRequest updatePostImageRequest) {
        List<String> reqUrls = updatePostImageRequest.getImagesToDelete();
        List<String> actuallyDeleted = deleteSelectedImagesDbOnly(postId, reqUrls);
        deleteS3QuietlyAfterCommit(actuallyDeleted);
    }

    @Transactional
    public List<String> deleteSelectedImagesDbOnly(Long postId, List<String> imagesToDelete) {
        if (imagesToDelete == null || imagesToDelete.isEmpty()) {
            appLogger.warn(new WarnLog("삭제 요청 이미지 없음")
                    .setTargetId(postId.toString()).setStatus("400"));
            throw new PostImageNotFoundException("삭제할 이미지 URL이 없습니다.");
        }
        List<PostImage> targets = postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete);
        try {
            if (!targets.isEmpty()) {
                postImageRepository.deleteAll(targets);
                appLogger.info(new InfoLog("선택 이미지 DB 삭제 완료")
                        .setTargetId(postId.toString())
                        .addDetails("count", String.valueOf(targets.size())));
            }

            return targets.stream().map(PostImage::getS3Url).collect(Collectors.toList());
        } catch (Exception e) {
            appLogger.error(new ErrorLog("DB 이미지 삭제 실패", e)
                    .setTargetId(postId.toString()).setStatus("500")
                    .addDetails("reason", e.getMessage()));
            throw new PostImageDeleteFailedException("이미지 삭제 중 문제가 발생했습니다.", e);
        }
    }

    public void deleteS3QuietlyAfterCommit(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    deleteS3ByUrlsQuietly(imageUrls);
                }
            });
        } else {
            deleteS3ByUrlsQuietly(imageUrls);
        }
    }

    public PostImage findFirstImageByPostId(Long postId) {
        return postImageRepository.findTopByPostIdOrderByIdAsc(postId);
    }


    public List<PostImageResponse> findImagesByPostId(Long postId) {
        log.debug("이미지 목록 조회: postId={}", postId);
        List<PostImage> postImages = postImageRepository.findByPostId(postId);
        return postImages.stream()
                .map(PostImageResponse::from)
                .collect(Collectors.toList());
    }

    public List<String> uploadFiles(List<MultipartFile> files, String pathPrefix) {
        return files.stream()
                .map(file -> s3Service.uploadFile(file, pathPrefix))
                .collect(Collectors.toList());
    }

}
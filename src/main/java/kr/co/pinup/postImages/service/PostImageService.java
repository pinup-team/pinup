package kr.co.pinup.postImages.service;

import jakarta.transaction.Transactional;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.custom.s3.exception.ImageDeleteFailedException;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.exception.postimage.PostImageSaveFailedException;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.model.dto.PostImageUploadRequest;
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

    @Transactional
    public List<PostImage> savePostImages(PostImageUploadRequest postImageUploadRequest, Post post) {
        if (postImageUploadRequest.getImages() == null || postImageUploadRequest.getImages().isEmpty()) {
            throw new PostImageNotFoundException("업로드할 이미지가 없습니다.");
        }

        List<String> imageUrls = uploadFiles(postImageUploadRequest.getImages(),PATH_PREFIX);

        List<PostImage> postImages = imageUrls.stream()
                .map(s3Url -> new PostImage(post, s3Url))
                .collect(Collectors.toList());

        try {
            postImageRepository.saveAll(postImages);
            appLogger.info(new InfoLog("이미지 저장 완료")
                    .setStatus("201")
                    .setTargetId(post.getId().toString())
                    .addDetails("count", String.valueOf(postImages.size())));
        } catch (Exception e) {
            appLogger.error(new ErrorLog("이미지 저장 실패", e)
                    .setStatus("500")
                    .setTargetId(post.getId().toString())
                    .addDetails("reason", e.getMessage()));
            throw new PostImageSaveFailedException("이미지 저장 중 문제가 발생했습니다.", e);
        }

        return postImages;
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

    public void deleteSelectedImages(Long postId, UpdatePostImageRequest updatePostImageRequest) {
        List<String> imagesToDelete = updatePostImageRequest.getImagesToDelete();

        if (imagesToDelete != null && !imagesToDelete.isEmpty()) {
            List<PostImage> postImages = postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete);

            postImages.forEach(postImage -> {
                String fileUrl = postImage.getS3Url();
                String fileName = PATH_PREFIX+ "/" + s3Service.extractFileName(fileUrl);
                try {
                    s3Service.deleteFromS3(fileName);
                } catch (ImageDeleteFailedException e) {
                    appLogger.error(new ErrorLog("S3 이미지 삭제 실패", e)
                            .setTargetId(postId.toString())
                            .setStatus("500")
                            .addDetails("file", fileName));
                    throw new ImageDeleteFailedException("이미지 삭제 중 문제가 발생했습니다.", e);
                }
            });

            try {
                postImageRepository.deleteAll(postImages);
                appLogger.info(new InfoLog("선택 이미지 삭제 완료")
                        .setTargetId(postId.toString())
                        .addDetails("count", String.valueOf(postImages.size())));
            } catch (Exception e) {
                appLogger.error(new ErrorLog("DB 이미지 삭제 실패", e)
                        .setTargetId(postId.toString())
                        .setStatus("500")
                        .addDetails("reason", e.getMessage()));
                throw new PostImageDeleteFailedException("이미지 삭제 중 문제가 발생했습니다.", e);
            }
        } else {
            appLogger.warn(new WarnLog("삭제 요청 이미지 없음")
                    .setTargetId(postId.toString())
                    .setStatus("400"));
            throw new PostImageNotFoundException("삭제할 이미지 URL이 없습니다.");
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
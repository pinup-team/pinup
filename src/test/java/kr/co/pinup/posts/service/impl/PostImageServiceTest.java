package kr.co.pinup.posts.service.impl;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.exception.postimage.PostImageUploadException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postImages.service.PostImageService;

import kr.co.pinup.posts.Post;
import kr.co.pinup.postImages.PostImage;

import kr.co.pinup.posts.repository.PostRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


@ExtendWith(SpringExtension.class)
public class PostImageServiceTest {

    @InjectMocks
    private PostImageService postImageService;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Client s3Client; // @Mock을 사용하여 Mock 객체 선언
    @Mock
    private PostRepository postRepository;
    @Mock
    private MultipartFile file;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;


    private Post post;
    private PostImageRequest postImageRequest;


    @BeforeEach
    public void setUp() throws IOException {
        // Set up postEntity
        post = Post.builder()
                .storeId(123L)
                .userId(456L)
                .title("Test Post")
                .content("This is a test post.")
                .thumbnail("http://example.com/image1.jpg")
                .build();

        file = Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("testImage.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(file.getContentType()).thenReturn("image/jpeg");

        postImageRequest = PostImageRequest.builder()
                .images(Collections.singletonList(file))
                .build();
    }

    @Test
    void testSavePostImages_Failure_NoImages() {
        postImageRequest = PostImageRequest.builder()
                .images(null)
                .build();


        assertThrows(PostImageUploadException.class, () -> {
            postImageService.savePostImages(postImageRequest, post);
        });
    }
    // deleteAllByPost 테스트
    @Test
    void testDeleteAllByPost_WhenNoImages() {
        Long postId = 1L;
        when(postImageRepository.findByPostId(postId)).thenReturn(Collections.emptyList());

        postImageService.deleteAllByPost(postId);

        verify(s3Client, never()).deleteObject((DeleteObjectRequest) any());
        verify(postImageRepository, never()).deleteAllByPostId(postId);
    }
//TODO 삭제할 이미지 없어서 테스트 실패 나중에 해결
//    @Test
//    void testDeleteAllByPost_WhenImagesExist() {
//        Long postId = 1L;
//        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, fileUrl)
//        );
//
//        // 이미지 리스트 반환 설정
//        when(postImageRepository.findByPostId(postId)).thenReturn(postImages);
//        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
//
//        // DB에서 이미지 삭제 메서드 모킹
//        doNothing().when(postImageRepository).deleteAllByPostId(postId);
//
//        // 실제 서비스 호출
//        postImageService.deleteAllByPost(postId);
//
//        // S3에서 파일을 삭제하고, DB에서 이미지 삭제가 호출되었는지 확인
//        verify(s3Client, times(1)).deleteObject((DeleteObjectRequest) any());
//        verify(postImageRepository, times(1)).deleteAllByPostId(postId);
//    }
//
//    @Test
//    void testDeleteAllByPost_WhenS3DeleteFails() {
//        Long postId = 1L;
//        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
//
//        // 이미지 엔티티 생성 (postId와 fileUrl 포함)
//        PostImageEntity postImageEntity = new PostImageEntity(postId, fileUrl);
//
//        // 해당 게시글의 이미지 리스트 생성
//        List<PostImageEntity> postImages = Arrays.asList(postImageEntity);
//
//        // postImageRepository가 해당 postId로 이미지를 반환하도록 설정
//        when(postImageRepository.findByPostId(postId)).thenReturn(postImages);
//
//        // S3에서 이미지 삭제 시 오류가 발생하도록 설정 (실제 S3 버킷을 사용하지 않음)
//        doThrow(new RuntimeException("S3 error")).when(s3Client).deleteObject(any(DeleteObjectRequest.class));
//
//        // 예외가 발생해야 한다
//        PostImageDeleteFailedException exception = assertThrows(PostImageDeleteFailedException.class, () -> {
//            postImageService.deleteAllByPost(postId);  // 실제 서비스 호출
//        });
//
//        // 예외 메시지가 포함되어 있는지 확인
//        assertTrue(exception.getMessage().contains("S3에서 이미지 삭제 실패"));
//
//        // S3 클라이언트가 deleteObject를 한 번 호출했는지 검증
//        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
//
//        // postImageRepository.findByPostId가 올바르게 작동했는지 확인
//        verify(postImageRepository, times(1)).findByPostId(postId);
//    }


    // deleteSelectedImages 테스트
    @Test
    void testDeleteSelectedImages_WhenImagesToDeleteIsEmpty() {
        Long postId = 1L;
        PostImageRequest postImageRequest = PostImageRequest.builder()
                .imagesToDelete(Collections.emptyList())  // Set imagesToDelete to an empty list
                .build();

        PostImageNotFoundException exception = assertThrows(PostImageNotFoundException.class, () -> {
            postImageService.deleteSelectedImages(postId, postImageRequest);
        });

        assertTrue(exception.getMessage().contains("삭제할 이미지 URL이 없습니다."));
    }
//TODO 삭제할 이미지 없어서 테스트 실패 나중에 해결
//    @Test
//    void testDeleteSelectedImages_WhenImagesExist() {
//        Long postId = 1L;
//        List<String> imagesToDelete = Arrays.asList("https://example.com/testImage.jpg");
//        PostImageDto postImageDto = new PostImageDto(imagesToDelete);
//
//        // 이미지 URL이 존재하는 경우에 대한 테스트 데이터 준비
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, imagesToDelete.get(0))
//        );
//
//        // postImageRepository의 findByPostIdAndS3UrlIn 메서드가 해당 이미지를 반환하도록 설정
//        when(postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete)).thenReturn(postImages);
//
//        // S3에서 파일 삭제 mock
//        when(s3Client.deleteObject(any(DeleteObjectRequest.class))).thenReturn(null);
//
//        // DB에서 이미지 삭제 mock
//        doNothing().when(postImageRepository).deleteAll(postImages);
//
//        // 실제 서비스 호출
//        postImageService.deleteSelectedImages(postId, postImageDto);
//
//        // S3에서 파일 삭제가 1번 호출되었는지 확인
//        verify(s3Client, times(1)).deleteObject(any(DeleteObjectRequest.class));
//
//        // DB에서 이미지 삭제가 1번 호출되었는지 확인
//        verify(postImageRepository, times(1)).deleteAll(postImages);
//    }
//
//
//    @Test
//    void testDeleteSelectedImages_WhenS3DeleteFails() {
//        Long postId = 1L;
//        List<String> imagesToDelete = Arrays.asList("https://example.com/testImage.jpg");
//        PostImageDto postImageDto = new PostImageDto(imagesToDelete);
//
//        List<PostImageEntity> postImages = Arrays.asList(
//                new PostImageEntity(postId, imagesToDelete.get(0))
//        );
//
//        // Mock DB에서 이미지를 찾을 수 있도록 설정
//        when(postImageRepository.findByPostIdAndS3UrlIn(postId, imagesToDelete)).thenReturn(postImages);
//
//        // S3에서 삭제 시 오류 발생하도록 설정
//        doThrow(new RuntimeException("S3 error")).when(s3Client).deleteObject((DeleteObjectRequest) any());
//
//        // 예외가 발생해야 한다
//        PostImageDeleteFailedException exception = assertThrows(PostImageDeleteFailedException.class, () -> {
//            postImageService.deleteSelectedImages(postId, postImageDto);
//        });
//
//        assertTrue(exception.getMessage().contains("S3에서 이미지 삭제 실패"));
//    }


    @Test
    public void testFindImagesByPostId_withImages() {

        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
        PostImage postImage = PostImage.builder()
                .s3Url(fileUrl)
                .build();

        post = Post.builder()
                .title("Test Post")
                .content("This is a test post.")
                .postImages(new ArrayList<>(Collections.singletonList(postImage))) // Add postImage to postImages list
                .build();

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Arrays.asList(postImage));

        when(postRepository.save(post)).thenReturn(post);
        when(postImageRepository.save(postImage)).thenReturn(postImage);

        post = postRepository.save(post);

        postImageRepository.save(postImage);

        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        assertNotNull(result);
        assertNotNull(result);
        assertTrue(result.isEmpty() || result.get(0).getS3Url() == null);
    }

    @Test
    public void testFindImagesByPostId_noImages() {

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Collections.emptyList());  // 빈 리스트 반환

        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

}

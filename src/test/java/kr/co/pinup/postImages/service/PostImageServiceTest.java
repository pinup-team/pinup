package kr.co.pinup.postImages.service;

import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.custom.s3.exception.s3.ImageDeleteFailedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
public class PostImageServiceTest {

    @InjectMocks
    private PostImageService postImageService;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Service s3Service;
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
        // Set up Store and Member objects
        Store store = Store.builder()
                .name("Test Store")
                .description("This is a test store.")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusMonths(6))
                .status(Status.PENDING)
                .imageUrl("http://example.com/store.jpg")
                .build();

        Member member = Member.builder()
                .email("test@domain.com")
                .name("Test User")
                .nickname("TestUser")
                .role(MemberRole.ROLE_USER)
                .build();

        // Set up PostEntity using Store and Member objects
        post = Post.builder()
                .store(store)
                .member(member)
                .title("Test Post")
                .content("This is a test post.")
                .thumbnail("http://example.com/image1.jpg")
                .build();

        // Mock MultipartFile for image
        file = Mockito.mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("testImage.jpg");
        when(file.getInputStream()).thenReturn(new ByteArrayInputStream("test data".getBytes()));
        when(file.getContentType()).thenReturn("image/jpeg");

        postImageRequest = PostImageRequest.builder()
                .images(Collections.singletonList(file))
                .build();
    }


    @DisplayName("이미지가 없는 경우 이미지 저장 실패")
    @Test
    void testSavePostImages_Failure_NoImages() {
        postImageRequest = PostImageRequest.builder()
                .images(null)
                .build();

        assertThrows(PostImageNotFoundException.class, () -> {
            postImageService.savePostImages(postImageRequest, post);
        });
    }

    @DisplayName("게시물에 이미지가 없을 때 전체 삭제 실행 안 됨")
    @Test
    void testDeleteAllByPost_WhenNoImages() {
        Long postId = 1L;

        when(postImageRepository.findByPostId(postId)).thenReturn(Collections.emptyList());

        postImageService.deleteAllByPost(postId);

        verify(s3Service, never()).deleteFromS3(anyString());

        verify(postImageRepository, never()).deleteAllByPostId(postId);
    }

    @DisplayName("S3 삭제 성공 시 이미지 전체 삭제 성공")
    @Test
    public void testDeleteAllByPost_WhenS3DeleteSucceeds() {

        String imageUrl = "https://example.com/image1.jpg";
        String fileName = "image1.jpg";
        PostImage image1 = new PostImage(post, imageUrl);

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Collections.singletonList(image1));

        doReturn(fileName).when(s3Service).extractFileName(imageUrl);

        doNothing().when(s3Service).deleteFromS3(fileName);

        postImageService.deleteAllByPost(post.getId());

        verify(s3Service).deleteFromS3(fileName);

        verify(postImageRepository).deleteAllByPostId(post.getId());
    }

    @DisplayName("S3 삭제 실패 시 이미지 전체 삭제 예외 발생")
    @Test
    public void testDeleteAllByPost_WhenS3DeleteFails() {

        String imageUrl = "https://example.com/image1.jpg";
        String fileName = "image1.jpg";
        PostImage image1 = new PostImage(post, imageUrl);

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Collections.singletonList(image1));

        when(s3Service.extractFileName(imageUrl)).thenReturn(fileName);

        doThrow(new ImageDeleteFailedException("S3에서 파일 삭제 실패"))
                .when(s3Service).deleteFromS3(fileName);

        PostImageDeleteFailedException exception = assertThrows(PostImageDeleteFailedException.class, () ->
                postImageService.deleteAllByPost(post.getId()));

        assertTrue(exception.getMessage().contains("이미지 삭제 중 문제가 발생했습니다."));

        verify(postImageRepository, never()).deleteAllByPostId(post.getId());

        verify(s3Service).deleteFromS3(fileName);
    }

    @DisplayName("선택한 이미지 삭제 - 삭제할 이미지가 없는 경우 예외 발생")
    @Test
    void testDeleteSelectedImages_WhenImagesToDeleteIsEmpty() {
        Long postId = 1L;
        PostImageRequest postImageRequest = PostImageRequest.builder()
                .imagesToDelete(Collections.emptyList())
                .build();

        PostImageNotFoundException exception = assertThrows(PostImageNotFoundException.class, () -> {
            postImageService.deleteSelectedImages(postId, postImageRequest);
        });

        assertTrue(exception.getMessage().contains("삭제할 이미지 URL이 없습니다."));
    }

    @DisplayName("선택한 이미지 삭제 - 존재하는 이미지 삭제 성공")
    @Test
    public void testDeleteSelectedImages_WhenImagesExist() {
        String imageUrl = "https://example.com/image1.jpg";
        PostImage image = new PostImage(post, imageUrl);

        when(postImageRepository.findByPostIdAndS3UrlIn(post.getId(), Collections.singletonList(imageUrl)))
                .thenReturn(Collections.singletonList(image));

        String fileName = "image1.jpg";
        when(s3Service.extractFileName(imageUrl)).thenReturn(fileName);

        PostImageRequest request = PostImageRequest.builder()
                .imagesToDelete(Collections.singletonList(imageUrl))
                .build();

        postImageService.deleteSelectedImages(post.getId(), request);

        verify(s3Service, times(1)).deleteFromS3(fileName);
        verify(postImageRepository, times(1)).deleteAll(Collections.singletonList(image));
    }

    @DisplayName("선택한 이미지 삭제 - S3 삭제 실패 시 예외 발생")
    @Test
    public void testDeleteSelectedImages_WhenS3DeleteFails() {
        String imageUrl = "https://example.com/image1.jpg";
        String fileName = "image1.jpg";
        PostImage image = new PostImage(post, imageUrl);

        when(postImageRepository.findByPostIdAndS3UrlIn(post.getId(), Collections.singletonList(imageUrl)))
                .thenReturn(Collections.singletonList(image));

        when(s3Service.extractFileName(imageUrl)).thenReturn(fileName);

        doThrow(new ImageDeleteFailedException("S3에서 파일 삭제 실패: " + fileName)).when(s3Service).deleteFromS3(fileName);

        ImageDeleteFailedException exception = assertThrows(ImageDeleteFailedException.class, () -> {
            PostImageRequest request = PostImageRequest.builder()
                    .imagesToDelete(Collections.singletonList(imageUrl))
                    .build();

            postImageService.deleteSelectedImages(post.getId(), request);
        });

        assertTrue(exception.getMessage().contains("이미지 삭제 중 문제가 발생했습니다."));

        verify(s3Service, times(1)).deleteFromS3(fileName);

        verify(postImageRepository, never()).deleteAll(anyList());
    }

    @DisplayName("게시글 ID로 이미지 조회 - 이미지가 있는 경우")
    @Test
    public void testFindImagesByPostId_withImages() {
        String fileUrl = "https://s3.amazonaws.com/testBucket/testImage.jpg";
        PostImage postImage = PostImage.builder()
                .s3Url(fileUrl)
                .post(post)
                .build();

        post = Post.builder()
                .title("Test Post")
                .content("test post.")
                .postImages(new ArrayList<>(Collections.singletonList(postImage)))
                .build();

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Arrays.asList(postImage));
        when(postRepository.save(post)).thenReturn(post);
        when(postImageRepository.save(postImage)).thenReturn(postImage);

        post = postRepository.save(post);
        postImageRepository.save(postImage);

        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        assertNotNull(result, "Result should not be null");
        assertFalse(result.isEmpty(), "Result should contain at least one image");
        assertNotNull(result.get(0).getS3Url(), "S3 URL should not be null");
        assertEquals(fileUrl, result.get(0).getS3Url(), "The S3 URL should match the expected URL");
    }

    @DisplayName("게시글 ID로 이미지 조회 - 이미지가 없는 경우")
    @Test
    public void testFindImagesByPostId_noImages() {

        when(postImageRepository.findByPostId(post.getId())).thenReturn(Collections.emptyList());  // 빈 리스트 반환

        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

}

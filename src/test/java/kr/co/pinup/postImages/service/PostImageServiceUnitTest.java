package kr.co.pinup.postImages.service;

import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.custom.s3.exception.s3.ImageDeleteFailedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.exception.postimage.PostImageDeleteFailedException;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageRequest;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.stores.Store;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostImageServiceUnitTest {

    @InjectMocks
    private PostImageService postImageService;

    @Mock
    private PostImageRepository postImageRepository;

    @Mock
    private S3Service s3Service;

    private Post mockPost;

    @BeforeEach
    void setUp() {
        Member member = Member.builder().nickname("행복한 돼지").build();
        Store store = Store.builder().name("Test Store").build();

        mockPost = Post.builder()
                .member(member)
                .store(store)
                .title("title")
                .content("content")
                .build();
    }

    @Test
    @DisplayName("이미지가 없는 경우 이미지 저장 실패")
    void testSavePostImages_Failure_NoImages() {
        PostImageRequest request = PostImageRequest.builder().images(null).build();

        assertThrows(PostImageNotFoundException.class,
                () -> postImageService.savePostImages(request, mockPost));
    }

    @Test
    @DisplayName("전체 삭제 - 이미지 없음")
    void testDeleteAllByPost_NoImages() {
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(Collections.emptyList());

        postImageService.deleteAllByPost(mockPost.getId());

        verify(s3Service, never()).deleteFromS3(anyString());
        verify(postImageRepository, never()).deleteAllByPostId(anyLong());
    }

    @Test
    @DisplayName("전체 삭제 - S3 삭제 실패 시 예외 발생")
    void testDeleteAllByPost_S3DeleteFails() {
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn("img.jpg");
        doThrow(new ImageDeleteFailedException("fail")).when(s3Service).deleteFromS3("img.jpg");

        assertThrows(PostImageDeleteFailedException.class,
                () -> postImageService.deleteAllByPost(mockPost.getId()));
    }

    @Test
    @DisplayName("전체 삭제 - S3 삭제 성공")
    void testDeleteAllByPost_Success() {
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn("img.jpg");

        postImageService.deleteAllByPost(mockPost.getId());

        verify(s3Service).deleteFromS3("img.jpg");
        verify(postImageRepository).deleteAllByPostId(mockPost.getId());
    }

    @Test
    @DisplayName("선택 이미지 삭제 - 비어있을 때 예외")
    void testDeleteSelectedImages_EmptyList() {
        PostImageRequest request = PostImageRequest.builder()
                .imagesToDelete(Collections.emptyList()).build();

        assertThrows(PostImageNotFoundException.class,
                () -> postImageService.deleteSelectedImages(mockPost.getId(), request));
    }

    @Test
    @DisplayName("선택 이미지 삭제 - 성공")
    void testDeleteSelectedImages_Success() {
        String url = "https://s3.com/img.jpg";
        String file = "img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostIdAndS3UrlIn(mockPost.getId(), List.of(url)))
                .thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn(file);

        PostImageRequest request = PostImageRequest.builder()
                .imagesToDelete(List.of(url)).build();

        postImageService.deleteSelectedImages(mockPost.getId(), request);

        verify(s3Service).deleteFromS3(file);
        verify(postImageRepository).deleteAll(List.of(img));
    }

    @Test
    @DisplayName("선택 이미지 삭제 - 실패")
    void testDeleteSelectedImages_S3Fails() {
        String url = "https://s3.com/img.jpg";
        String file = "img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostIdAndS3UrlIn(mockPost.getId(), List.of(url)))
                .thenReturn(List.of(img));
        when(s3Service.extractFileName(url)).thenReturn(file);
        doThrow(new ImageDeleteFailedException("fail")).when(s3Service).deleteFromS3(file);

        PostImageRequest request = PostImageRequest.builder()
                .imagesToDelete(List.of(url)).build();

        assertThrows(ImageDeleteFailedException.class,
                () -> postImageService.deleteSelectedImages(mockPost.getId(), request));
    }

    @Test
    @DisplayName("게시글 ID로 이미지 조회 - 이미지 없음")
    void testFindImagesByPostId_NoImages() {
        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(Collections.emptyList());

        List<PostImageResponse> result = postImageService.findImagesByPostId(mockPost.getId());

        assertNotNull(result);
        assertEquals(0, result.size());
    }

    @Test
    @DisplayName("게시글 ID로 이미지 조회 - 이미지 있음")
    void testFindImagesByPostId_WithImages() {
        String url = "https://s3.com/img.jpg";
        PostImage img = new PostImage(mockPost, url);

        when(postImageRepository.findByPostId(mockPost.getId())).thenReturn(List.of(img));

        List<PostImageResponse> result = postImageService.findImagesByPostId(mockPost.getId());

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(url, result.get(0).getS3Url());
    }
}

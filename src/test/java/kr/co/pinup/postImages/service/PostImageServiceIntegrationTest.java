package kr.co.pinup.postImages.service;


import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.custom.s3.exception.ImageDeleteFailedException;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import org.springframework.test.context.transaction.TestTransaction;
import kr.co.pinup.postImages.exception.postimage.PostImageNotFoundException;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.model.dto.UpdatePostImageRequest;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(PostImageServiceIntegrationTest.TestMockConfig.class)
@Transactional
class PostImageServiceIntegrationTest {

    @Autowired private PostImageService postImageService;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;

    @Autowired
    private S3Service s3Service;

    private Post post;

    @TestConfiguration
    static class TestMockConfig {
        @Bean(name = "s3Service")
        @Primary
        public S3Service s3Service() {
            return mock(S3Service.class);
        }
    }

    @BeforeEach
    void setUp() {
        reset(s3Service);

        StoreCategory category = storeCategoryRepository.save(new StoreCategory("Test Category"));

        Location location = locationRepository.save(new Location("City", "00000", "State", "District", 0.0, 0.0, "Address", "Detail"));

        Store store = storeRepository.save(Store.builder()
                .name("Test Store")
                .description("Store Desc")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());

        Member member = memberRepository.save(Member.builder()
                .email("user@test.com")
                .name("User")
                .nickname("테스터_" + UUID.randomUUID())
                .password("encoded")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver-123")
                .role(MemberRole.ROLE_USER)
                .build());

        post = postRepository.save(Post.builder()
                .title("Post Title")
                .content("Post Content")
                .thumbnail("thumb.jpg")
                .member(member)
                .store(store)
                .build());
    }

    @Test
    @DisplayName("게시글 ID로 이미지가 없는 경우 빈 리스트 반환")
    void getImagesByPostId_whenNoImages_thenReturnsEmptyList() {
        // When
        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        // Then
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("게시글 ID로 이미지 조회 - 이미지 있음")
    void getImagesByPostId_whenImagesExist_thenReturnsImageList() {
        // Given
        PostImage image = new PostImage(post, "https://s3.com/test.jpg");
        postImageRepository.save(image);

        // When
        List<PostImageResponse> result = postImageService.findImagesByPostId(post.getId());

        // Then
        assertEquals(1, result.size());
        assertEquals("https://s3.com/test.jpg", result.get(0).getS3Url());
    }

    @Test
    @DisplayName("전체 삭제 - 이미지 있음")
    void deleteAllImages_whenImagesExist_thenSuccess() {
        // Given
        PostImage image = postImageRepository.save(new PostImage(post, "https://s3.com/test.jpg"));
        when(s3Service.extractFileName(image.getS3Url())).thenReturn("test.jpg");

        // When
        postImageService.deleteAllByPost(post.getId());

        // 커밋 -> afterCommit 콜백 실행 지점
        assertTrue(TestTransaction.isActive());
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Then: 커밋 이후에 검증
        verify(s3Service, times(1)).deleteFromS3("post/test.jpg");

        // DB 검증은 새 트랜잭션에서
        TestTransaction.start();
        assertTrue(postImageRepository.findByPostId(post.getId()).isEmpty());
    }

    @Test
    @DisplayName("전체 삭제 - S3 실패해도 서비스는 예외 미전파(quiet)")
    void deleteAllImages_whenS3Fails_thenDoesNotThrow_andDeletesDb() {
        // Given
        PostImage image = postImageRepository.save(new PostImage(post, "https://s3.com/test.jpg"));
        when(s3Service.extractFileName(image.getS3Url())).thenReturn("test.jpg");
        doThrow(new ImageDeleteFailedException("삭제 실패"))
                .when(s3Service).deleteFromS3("post/test.jpg");

        // When (quiet 정책)
        assertDoesNotThrow(() -> postImageService.deleteAllByPost(post.getId()));

        // 커밋해서 afterCommit 실행
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Then
        verify(s3Service).deleteFromS3("post/test.jpg");
        TestTransaction.start();
        assertTrue(postImageRepository.findByPostId(post.getId()).isEmpty());
    }

    @Test
    @DisplayName("선택 이미지 삭제 성공")
    void deleteSelectedImages_whenValidRequest_thenSuccess() {
        // Given
        String imageUrl = "https://s3.com/img.jpg";
        postImageRepository.save(new PostImage(post, imageUrl));
        when(s3Service.extractFileName(imageUrl)).thenReturn("img.jpg");

        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(List.of(imageUrl))
                .build();

        // When
        postImageService.deleteSelectedImages(post.getId(), request);

        // 커밋 -> afterCommit 실행
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Then
        verify(s3Service).deleteFromS3("post/img.jpg");
        TestTransaction.start();
        assertTrue(postImageRepository.findByPostId(post.getId()).isEmpty());
    }

    @Test
    @DisplayName("선택 이미지 삭제 - S3 실패해도 서비스는 예외 미전파(quiet)")
    void deleteSelectedImages_whenS3Fails_thenDoesNotThrow_andDeletesDb() {
        // Given
        String imageUrl = "https://s3.com/img.jpg";
        postImageRepository.save(new PostImage(post, imageUrl));
        when(s3Service.extractFileName(imageUrl)).thenReturn("img.jpg");
        doThrow(new ImageDeleteFailedException("삭제 실패"))
                .when(s3Service).deleteFromS3("post/img.jpg");

        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(List.of(imageUrl))
                .build();

        // When (quiet 정책)
        assertDoesNotThrow(() -> postImageService.deleteSelectedImages(post.getId(), request));

        // 커밋 -> afterCommit 실행
        TestTransaction.flagForCommit();
        TestTransaction.end();

        // Then
        verify(s3Service).deleteFromS3("post/img.jpg");
        TestTransaction.start();
        assertTrue(postImageRepository.findByPostId(post.getId()).isEmpty());
    }


    @Test
    @DisplayName("선택 이미지 삭제 실패 - 삭제 리스트 없음")
    void deleteSelectedImages_whenEmptyRequest_thenThrowsException() {
        // Given
        UpdatePostImageRequest request = UpdatePostImageRequest.builder()
                .imagesToDelete(Collections.emptyList())
                .build();

        // When & Then
        assertThrows(PostImageNotFoundException.class, () ->
                postImageService.deleteSelectedImages(post.getId(), request));
    }
}


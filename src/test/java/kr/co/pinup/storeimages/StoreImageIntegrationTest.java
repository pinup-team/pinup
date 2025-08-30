package kr.co.pinup.storeimages;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.s3.S3Service;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.storeimages.repository.StoreImageRepository;
import kr.co.pinup.storeimages.service.StoreImageService;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.containers.localstack.LocalStackContainer.Service;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
public class StoreImageIntegrationTest {

    static final DockerImageName image = DockerImageName.parse("gresau/localstack-persist:latest")
            .asCompatibleSubstituteFor("localstack/localstack");

    @Container
    static LocalStackContainer localstack = new LocalStackContainer(image)
            .withServices(Service.S3);

    @MockitoBean
    private AppLogger appLogger;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreImageRepository storeImageRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private StoreCategoryRepository storeCategoryRepository;

    @Autowired
    private StoreImageService storeImageService;

    @Autowired
    private S3Service s3Service;

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("cloud.aws.s3.bucket", () -> "pinup-test");
        registry.add("cloud.aws.region.static", () -> localstack.getRegion());
        registry.add("cloud.aws.credentials.accessKey", () -> localstack.getAccessKey());
        registry.add("cloud.aws.credentials.secretKey", () -> localstack.getSecretKey());
        registry.add("cloud.aws.s3.endpoint", () -> localstack.getEndpointOverride(Service.S3).toString());
    }

    @DisplayName("S3에 업로드된 이미지들의 url을 받아 스토어 이미지들을 저장한다.")
    @Test
    void createUploadImages() {
        // Arrange
        final S3Client s3Client = S3Client.builder()
                .endpointOverride(localstack.getEndpointOverride(Service.S3))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey())
                ))
                .region(Region.of(localstack.getRegion()))
                .build();
        s3Client.createBucket(CreateBucketRequest.builder()
                .bucket("pinup-test")
                .build());

        final Location location = locationRepository.save(getLocation());
        final StoreCategory storeCategory = storeCategoryRepository.save(getStoreCategory());
        final Store store = storeRepository.save(getStore(storeCategory, location));

        final List<MultipartFile> images = List.of(
                new MockMultipartFile("file", "test1.jpg", "image/jpeg", "image1".getBytes()),
                new MockMultipartFile("file", "test2.jpg", "image/jpeg", "image2".getBytes())
        );
        final long thumbnailIndex = 1L;

        // Act
        final List<StoreImage> result = storeImageService.createUploadImages(store, images, thumbnailIndex);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result.get((int) thumbnailIndex).isThumbnail()).isTrue();
        assertThat(result.get(0).getImageUrl()).contains("test1.jpg");

        final ListObjectsV2Response list = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket("pinup-test")
                .build());

        assertThat(list.contents()).hasSize(2);
    }

    private StoreCategory getStoreCategory() {
        return StoreCategory.builder()
                .name("패션")
                .build();
    }

    private Location getLocation() {
        return Location.builder()
                .name("서울 송파구 올림픽로 300")
                .zonecode("05551")
                .sido("서울")
                .sigungu("송파구")
                .address("서울 송파구 올림픽로 300")
                .longitude(127.104302)
                .latitude(37.513713)
                .build();
    }

    private Store getStore(final StoreCategory storeCategory, final Location location) {
        return Store.builder()
                .name("팝업스토어")
                .description("팝업스토어가 올해도 여러분을 찾아갑니다!")
                .category(storeCategory)
                .location(location)
                .startDate(LocalDate.of(2025, 7, 1))
                .endDate(LocalDate.of(2025, 7, 7))
                .storeStatus(StoreStatus.PENDING)
                .build();
    }
}

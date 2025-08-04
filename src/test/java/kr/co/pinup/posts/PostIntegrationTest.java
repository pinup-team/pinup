package kr.co.pinup.posts;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.S3ClientConfig;
import kr.co.pinup.locations.Location;
import kr.co.pinup.locations.reposiotry.LocationRepository;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.PostImage;
import kr.co.pinup.postImages.repository.PostImageRepository;
import kr.co.pinup.postLikes.PostLike;
import kr.co.pinup.postLikes.repository.PostLikeRepository;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.storecategories.StoreCategory;
import kr.co.pinup.storecategories.repository.StoreCategoryRepository;
import kr.co.pinup.stores.Store;
import kr.co.pinup.stores.model.enums.StoreStatus;
import kr.co.pinup.stores.repository.StoreRepository;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.ui.ModelMap;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import({S3ClientConfig.class, PostIntegrationTest.TestMockConfig.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PostIntegrationTest {

    @TestConfiguration
    static class TestMockConfig {
        @Bean
        @Primary
        public MemberService memberService() {
            MemberService mock = mock(MemberService.class);
            when(mock.isAccessTokenExpired(any(), any())).thenReturn(false);
            when(mock.refreshAccessToken(any())).thenReturn("mocked-token");
            return mock;
        }
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private PostRepository postRepository;
    @Autowired private PostImageRepository postImageRepository;
    @Autowired private PostLikeRepository postLikeRepository;
    @Autowired private StoreRepository storeRepository;
    @Autowired private StoreCategoryRepository storeCategoryRepository;
    @Autowired private LocationRepository locationRepository;
    @Autowired private MemberRepository memberRepository;
    @Autowired private S3Client s3Client;
    @Value("${cloud.aws.s3.bucket}") String bucketName;
    @Autowired PlatformTransactionManager txManager;

    private Store store;
    private Member member;
    private Post mockPost;

    @BeforeEach
    void setUp(TestInfo testInfo) {
        postLikeRepository.deleteAll();
        postImageRepository.deleteAll();
        postRepository.deleteAll();
        memberRepository.deleteAll();

        StoreCategory category = storeCategoryRepository.save(new StoreCategory("카테고리"));
        Location location = locationRepository.save(new Location("서울", "12345", "서울시", "강남구", 37.5, 127.0, "주소", "상세주소"));

        store = storeRepository.save(Store.builder()
                .name("테스트 스토어")
                .description("설명")
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(10))
                .storeStatus(StoreStatus.RESOLVED)
                .category(category)
                .location(location)
                .build());

        member = memberRepository.save(Member.builder()
                .email("user@test.com")
                .name("유저")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver123")
                .role(MemberRole.ROLE_USER)
                .build());

        if (testInfo.getTags().contains("anonymous-update")) {
            mockPost = postRepository.save(Post.builder().title("제목").content("내용").store(store).member(member).build());
            postImageRepository.save(PostImage.builder().post(mockPost).s3Url("url.jpg").build());
        }

    }

    @AfterAll
    void cleanUpS3AfterTests() {
        ListObjectsV2Response list = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("post/")
                .build());

        if (!list.contents().isEmpty()) {
            List<ObjectIdentifier> targets = list.contents().stream()
                    .map(obj -> ObjectIdentifier.builder().key(obj.key()).build())
                    .toList();

            s3Client.deleteObjects(builder -> builder
                    .bucket(bucketName)
                    .delete(d -> d.objects(targets))
            );

            System.out.println("🧹 LocalStack 테스트 객체 삭제 완료: " + targets.size() + "개");
        } else {
            System.out.println("✅ 테스트용 S3 객체가 존재하지 않음");
        }
    }

    @Nested
    @DisplayName("게시글 작성 흐름 테스트")
    class CreatePost {

        @Test
        @DisplayName("정상적으로 게시글이 생성되고 모든 흐름이 작동한다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void createPost_thenFlowComplete() throws Exception {
            // Given: 이미지 파일과 게시글 정보가 주어짐
            MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "data2".getBytes());

            CreatePostRequest requestDto = new CreatePostRequest(
                    store.getId(),
                    "테스트 제목",
                    "테스트 내용"
            );
            String json = new ObjectMapper().writeValueAsString(requestDto);

            MockMultipartFile postPart = new MockMultipartFile(
                    "post", "post.json", "application/json", json.getBytes(StandardCharsets.UTF_8)
            );

            // When: 게시글 생성 요청
            mockMvc.perform(multipart("/api/post/create")
                            .file(image1)
                            .file(image2)
                            .file(postPart)
                            .with(csrf()))
                    // Then: 응답이 성공이고 JSON 응답이 예상한 값과 일치함
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("테스트 제목"))
                    .andExpect(jsonPath("$.thumbnail").value(containsString("img1.jpg")));

            // And: DB 및 S3 상태 검증
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);
            txTemplate.executeWithoutResult(status -> {
                List<Post> posts = postRepository.findAll();
                Post saved = posts.get(0);
                Hibernate.initialize(saved.getPostImages());

                assertThat(saved.getPostImages()).hasSize(2);
                assertThat(saved.getThumbnail()).contains("img1.jpg");

                for (PostImage img : saved.getPostImages()) {
                    String key = extractS3Key(img.getS3Url());
                    assertS3ObjectExists(key);
                }
            });
        }

    }

    @Nested
    @DisplayName("게시글 삭제 흐름 테스트")
    class DeletePost {

        @Test
        @DisplayName("게시글 삭제 요청 시 DB에서 삭제되고 이미지도 함께 삭제된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
        void deletePost_thenCascadeDeleteWorks() throws Exception {
            // Given: 게시글 생성
            Long postId = createPostAsLoggedInUser("삭제용 제목", "img1.jpg");

            // When + Then: 삭제 요청 실행 및 검증
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);
            txTemplate.executeWithoutResult(status -> {
                Post post = postRepository.findById(postId).orElseThrow();
                Hibernate.initialize(post.getPostImages());
                List<PostImage> images = post.getPostImages();
                assertThat(images).hasSize(2);

                new TransactionTemplate(txManager).executeWithoutResult(deleteTx -> {
                    try {
                        mockMvc.perform(delete("/api/post/" + postId).with(csrf()))
                                .andExpect(status().isNoContent());
                    } catch (Exception e) {
                        throw new RuntimeException("삭제 요청 실패", e);
                    }
                });

                // DB 삭제 확인
                assertThat(postRepository.findById(postId)).isEmpty();
                assertThat(postImageRepository.findByPostId(postId)).isEmpty();

                // S3 삭제 확인
                List<String> deletedKeys = images.stream()
                        .map(PostImage::getS3Url)
                        .map(PostIntegrationTest.this::extractS3Key)
                        .map(fileName -> "post/" + fileName)
                        .toList();
                assertS3ObjectsDeleted(deletedKeys);
            });
        }

    }

    @Nested
    @DisplayName("게시글 수정 흐름 테스트")
    class UpdatePost {

        @Test
        @DisplayName("기존 이미지를 삭제하고 새로운 이미지를 추가한 뒤 게시글을 수정한다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void updatePost_thenFlowComplete() throws Exception {
            // Given: 게시글이 생성되고 이미지가 등록됨
            Long postId = createPostAsLoggedInUser("초기 제목", "img1.jpg");
            String deleteTargetUrl = postImageRepository.findByPostId(postId).get(0).getS3Url();
            String deleteKey = extractS3Key(deleteTargetUrl);

            // When: 기존 이미지를 삭제하고 새 이미지를 업로드하여 수정 요청
            MockMultipartFile newImage = new MockMultipartFile("images", "img3.jpg", "image/jpeg", "data3".getBytes());
            mockMvc.perform(multipart("/api/post/" + postId)
                            .file(newImage)
                            .file(createUpdatePostRequestPart("수정된 제목", "수정된 내용"))
                            .param("imagesToDelete", deleteTargetUrl)
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"));

            // Then: 응답 성공 및 DB, S3 상태 검증
            TransactionTemplate txTemplate = new TransactionTemplate(txManager);
            txTemplate.executeWithoutResult(status -> {
                List<PostImage> remainingImages = postImageRepository.findByPostId(postId);
                List<String> imageUrls = remainingImages.stream().map(PostImage::getS3Url).toList();

                assertThat(remainingImages).hasSize(2);
                assertThat(imageUrls).doesNotContain(deleteTargetUrl);
                assertThat(postRepository.findById(postId).orElseThrow().getThumbnail())
                        .isEqualTo(imageUrls.get(0));

                // And: 삭제 대상 S3 객체가 실제로 삭제되었는지 확인
                assertS3ObjectsDeletedByUrls(List.of(deleteTargetUrl));
                for (PostImage img : remainingImages) {
                    assertS3ObjectExists(extractS3Key(img.getS3Url()));
                }
            });
        }

        @Test
        @DisplayName("새로운 이미지만 업로드하고 기존 이미지는 유지된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void updatePost_whenOnlyUploadImages_thenSuccess() throws Exception {
            // Given: 기존 이미지 2장이 등록된 게시글 생성
            Long postId = createPostAsLoggedInUser("초기 제목", "img1.jpg");

            // When: 새 이미지 업로드 (img3)
            MockMultipartFile image3 = new MockMultipartFile("images", "img3.jpg", "image/jpeg", "data3".getBytes());
            mockMvc.perform(multipart("/api/post/" + postId)
                            .file(image3)
                            .file(createUpdatePostRequestPart("업로드 테스트", "내용"))
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isOk());

            // Then: 이미지 3장이 등록되어 있고 썸네일이 img3로 변경됨
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.executeWithoutResult(status -> {
                Post updated = postRepository.findById(postId).orElseThrow();
                List<PostImage> allImages = postImageRepository.findByPostId(postId);

                assertThat(allImages).hasSize(3);
                assertThat(updated.getThumbnail()).isEqualTo(allImages.get(0).getS3Url());

                // And: 모든 S3 객체가 존재함
                for (PostImage img : allImages) {
                    assertS3ObjectExists(extractS3Key(img.getS3Url()));
                }
            });
        }

        @Test
        @DisplayName("제목만 수정하고 새로운 이미지를 업로드하면 기존 이미지는 유지되고 썸네일은 변경된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void updatePost_whenTitleAndUploadImages_thenSuccess() throws Exception {
            // Given: 게시글과 이미지가 생성됨
            Long postId = createPostAsLoggedInUser("초기 제목", "img1.jpg");

            // When: 제목만 변경하고 새 이미지(img3)를 업로드함
            MockMultipartFile image3 = new MockMultipartFile("images", "img3.jpg", "image/jpeg", "data3".getBytes());
            mockMvc.perform(multipart("/api/post/" + postId)
                            .file(image3)
                            .file(createUpdatePostRequestPart("수정된 제목", "내용"))
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("수정된 제목"));

            // Then: 총 3장 이미지가 등록되어 있고 썸네일이 img3로 변경됨
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.executeWithoutResult(status -> {
                Post updated = postRepository.findById(postId).orElseThrow();
                List<PostImage> allImages = postImageRepository.findByPostId(postId);

                assertThat(allImages).hasSize(3);
                assertThat(updated.getTitle()).isEqualTo("수정된 제목");
                assertThat(updated.getThumbnail()).isEqualTo(allImages.get(0).getS3Url());

                // And: 모든 S3 객체가 존재해야 함
                for (PostImage img : allImages) {
                    assertS3ObjectExists(extractS3Key(img.getS3Url()));
                }
            });
        }

        @Test
        @DisplayName("제목만 수정하고 이미지 1개만 삭제할 경우 예외가 발생하고 기존 상태가 유지된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void updatePost_whenTitleAndDeleteImages_thenThrowsExceptionAndRollback() throws Exception {
            // Given: 게시글과 이미지 2장이 생성됨
            Long postId = createPostAsLoggedInUser("초기 제목", "img1.jpg");
            List<PostImage> originalImages = postImageRepository.findByPostId(postId);
            String deleteTargetUrl = originalImages.get(0).getS3Url();
            String remainingUrl = originalImages.get(1).getS3Url();

            // When: 이미지 1개만 삭제 요청하여 예외 발생 유도
            MockMultipartFile empty = new MockMultipartFile("images", "", "application/octet-stream", new byte[0]);
            mockMvc.perform(multipart("/api/post/" + postId)
                            .file(empty)
                            .file(createUpdatePostRequestPart("예외 제목 수정", "내용"))
                            .param("imagesToDelete", deleteTargetUrl)
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isBadRequest())
                    .andExpect(result -> {
                        String exceptionName = result.getResolvedException().getClass().getSimpleName();
                        assertThat(exceptionName).isEqualTo("PostImageUpdateCountException");
                    });

            // Then: 트랜잭션이 롤백되어 기존 이미지들이 그대로 유지됨
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.executeWithoutResult(status -> {
                Post post = postRepository.findById(postId).orElseThrow();
                List<PostImage> currentImages = postImageRepository.findByPostId(postId);
                List<String> currentUrls = currentImages.stream().map(PostImage::getS3Url).toList();

                assertThat(currentUrls).containsExactlyInAnyOrder(
                        originalImages.get(0).getS3Url(),
                        originalImages.get(1).getS3Url()
                );
                assertThat(post.getThumbnail()).isEqualTo(originalImages.get(0).getS3Url());

                for (String url : currentUrls) {
                    assertS3ObjectExists(extractS3Key(url));
                }
            });
        }
    }

    @Nested
    @DisplayName("게시글 비활성화 흐름 테스트")
    class DisalbePost {

        @Test
        @DisplayName("게시글 비활성화 요청 시 isDeleted가 true로 변경된다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void disablePost_thenPostMarkedAsDeleted() throws Exception {
            // Given: 게시글이 생성됨
            Long postId = createPostAsLoggedInUser("비활성화 테스트", "img1.jpg");

            // When: 비활성화 요청 실행
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .patch("/api/post/" + postId + "/disable")
                                    .with(csrf()))
                    // Then: 요청이 성공하고 상태코드는 204
                    .andExpect(status().isNoContent());

            // And: DB에서 해당 게시글 isDeleted = true
            TransactionTemplate tx = new TransactionTemplate(txManager);
            tx.executeWithoutResult(status -> {
                Post disabledPost = postRepository.findById(postId).orElseThrow();
                assertThat(disabledPost.isDeleted()).isTrue();
            });
        }

        @Test
        @DisplayName("존재하지 않는 게시글에 대해 비활성화 요청 시 예외가 발생한다")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
        void disablePost_whenPostNotFound_thenFail() throws Exception {
            // Given: 존재하지 않는 게시글 ID
            Long invalidId = 9999L;

            // When: 비활성화 요청 실행
            mockMvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                    .patch("/api/post/" + invalidId + "/disable")
                                    .with(csrf()))
                    // Then: 404 Not Found 응답
                    .andExpect(status().isNotFound())
                    // And: 예외 클래스명이 PostNotFoundException
                    .andExpect(result -> {
                        String exName = result.getResolvedException().getClass().getSimpleName();
                        assertThat(exName).isEqualTo("PostNotFoundException");
                    });
        }
    }

    @Nested
    @DisplayName("게시글 시큐리티 흐름 테스트")
    class SecurityPost {

        @Test
        @DisplayName("ROLE_USER가 게시글 삭제 요청 시 403 Forbidden")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void deletePost_whenUser_thenForbidden() throws Exception {
            // Given: 게시글 생성
            Long postId = createPostAsLoggedInUser("비활성화 테스트", "img1.jpg");

            // When: 삭제 요청 실행 (ROLE_USER)
            mockMvc.perform(delete("/api/post/" + postId).with(csrf()))
                    // Then: 403 Forbidden 응답
                    .andExpect(status().isForbidden());
        }

        @Tag("anonymous-update")
        @Test
        @DisplayName("비로그인 사용자가 게시글 수정 시도 시 401 isUnauthorized")
        void updatePost_whenAnonymous_thenUnauthorized() throws Exception {
            // Given: 비로그인 사용자가 게시글 접근 (mockPost는 사전 세팅됨)

            // When: 수정 요청 실행
            mockMvc.perform(multipart("/api/post/" + mockPost.getId())
                            .file(new MockMultipartFile("images", "", "application/octet-stream", new byte[0]))
                            .param("title", "변경됨")
                            .param("content", "내용")
                            .param("thumbnail", "img.jpg")
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    // Then: 401 Unauthorized 응답
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("존재하지 않는 게시글 삭제 시도 시 404 Not Found")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_ADMIN)
        void deletePost_whenNotFound_thenFail() throws Exception {
            // Given: 존재하지 않는 게시글 ID
            Long invalidId = 9999L;

            // When: 삭제 요청 실행
            mockMvc.perform(delete("/api/post/" + invalidId).with(csrf()))
                    // Then: 404 Not Found 응답
                    .andExpect(status().isNotFound())
                    // And: 예외 클래스명이 PostNotFoundException
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getClass().getSimpleName())
                                    .isEqualTo("PostNotFoundException")
                    );
        }

        @Test
        @DisplayName("존재하지 않는 게시글 수정 시도 시 404 Not Found")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void updatePost_whenNotFound_thenFail() throws Exception {
            // Given: 존재하지 않는 게시글 ID
            long nonExistentPostId = 9999L;

            // JSON part
            UpdatePostRequest request = UpdatePostRequest.builder()
                    .title("수정 제목")
                    .content("수정 내용")
                    .build();
            String json = new ObjectMapper().writeValueAsString(request);
            MockMultipartFile updatePostPart = new MockMultipartFile(
                    "updatePostRequest", "updatePostRequest.json", "application/json", json.getBytes(StandardCharsets.UTF_8)
            );

            // 빈 이미지
            MockMultipartFile emptyImages = new MockMultipartFile("images", "", "application/octet-stream", new byte[0]);

            // When & Then
            mockMvc.perform(multipart("/api/post/" + nonExistentPostId)
                            .file(updatePostPart)
                            .file(emptyImages)
                            .with(csrf())
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andExpect(status().isNotFound())
                    .andExpect(result ->
                            assertThat(result.getResolvedException().getClass().getSimpleName())
                                    .isEqualTo("PostNotFoundException"));
        }

    }

    @Nested
    @DisplayName("게시글 목록 리스트 흐름 테스트")
    class PostListLikeIntegration {

        @Test
        @DisplayName("로그인 사용자 - likedByCurrentUser=true 반환")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void getPostList_whenLiked_thenLikedByCurrentUserTrue() throws Exception {
            // Given
            Post post = postRepository.save(Post.builder()
                    .title("제목").content("내용").store(store).member(member).build());

            postLikeRepository.save(PostLike.builder()
                    .post(post).member(member).build());

            // When
            MvcResult result = mockMvc.perform(get("/post/list/" + store.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then: 모델에서 직접 검증
            ModelMap modelMap = result.getModelAndView().getModelMap();
            List<PostResponse> posts = (List<PostResponse>) modelMap.get("posts");

            assertThat(posts).hasSize(1);
            PostResponse response = posts.get(0);
            assertThat(response.likedByCurrentUser()).isTrue();
            assertThat(response.commentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("로그인 사용자 - 좋아요 하지 않은 경우 likedByCurrentUser=false")
        @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
        void getPostList_whenNotLiked_thenLikedByCurrentUserFalse() throws Exception {
            // Given
            postRepository.save(Post.builder()
                    .title("제목")
                    .content("내용")
                    .store(store)
                    .member(member)
                    .build());

            // When
            MvcResult result = mockMvc.perform(get("/post/list/" + store.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            ModelMap modelMap = result.getModelAndView().getModelMap();
            List<PostResponse> posts = (List<PostResponse>) modelMap.get("posts");

            assertThat(posts).isNotEmpty();
            assertThat(posts).anySatisfy(post -> {
                assertThat(post.likedByCurrentUser()).isFalse();
                assertThat(post.commentCount()).isEqualTo(0);
            });
        }

        @Test
        @DisplayName("비로그인 사용자 - likedByCurrentUser=false 반환")
        void getPostList_whenAnonymous_thenLikedByCurrentUserFalse() throws Exception {
            // Given
            postRepository.save(Post.builder()
                    .title("비로그인용")
                    .content("내용")
                    .store(store)
                    .member(member)
                    .build());

            // When
            MvcResult result = mockMvc.perform(get("/post/list/" + store.getId()))
                    .andExpect(status().isOk())
                    .andReturn();

            // Then
            ModelMap modelMap = result.getModelAndView().getModelMap();
            List<PostResponse> posts = (List<PostResponse>) modelMap.get("posts");

            assertThat(posts).isNotEmpty();
            assertThat(posts).anySatisfy(post -> {
                assertThat(post.likedByCurrentUser()).isFalse();
                assertThat(post.commentCount()).isEqualTo(0);
            });
        }

    }

    private String extractS3Key(String url) {
        String[] urlParts = url.split("/");
        return urlParts[urlParts.length - 1];  // ex: 123abc_img1.jpg
    }

    private void assertS3ObjectExists(String key) {
        String fullKey = "post/" + key;
        ListObjectsV2Response list = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(fullKey)
                .build());

        assertThat(list.contents())
                .as("❌ [S3 객체가 존재하지 않음]: " + key)
                .anyMatch(obj -> obj.key().equals(fullKey));
    }

    private void assertS3ObjectsDeletedByUrls(List<String> s3Urls) {
        List<String> keys = s3Urls.stream()
                .map(this::extractS3Key)
                .toList();
        assertS3ObjectsDeleted(keys);
    }

    private void assertS3ObjectsDeleted(List<String> keys) {
        ListObjectsV2Response list = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix("post/")
                .build());

        Set<String> existingKeys = list.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toSet());

        assertThat(existingKeys)
                .as("❌ [삭제되지 않은 S3 객체 있음]: " + keys)
                .doesNotContainAnyElementsOf(keys);
    }

    private Long createPostAsLoggedInUser(String title, String thumbnailName) throws Exception {
        MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "data1".getBytes());
        MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "data2".getBytes());
        CreatePostRequest postRequest = new CreatePostRequest(
                store.getId(),
                title,
                "내용"
        );
        String postJson = new ObjectMapper().writeValueAsString(postRequest);
        MockMultipartFile postPart = new MockMultipartFile("post", "post.json", "application/json", postJson.getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/post/create")
                        .file(image1)
                        .file(image2)
                        .file(postPart)
                        .param("thumbnail", thumbnailName)
                        .with(csrf()))
                .andExpect(status().isCreated());

        return postRepository.findAll().get(0).getId();
    }

    private MockMultipartFile createUpdatePostRequestPart(String title, String content) throws Exception {
        UpdatePostRequest request = UpdatePostRequest.builder()
                .title(title)
                .content(content)
                .build();
        String json = new ObjectMapper().writeValueAsString(request);
        return new MockMultipartFile(
                "updatePostRequest",
                "updatePostRequest.json",
                "application/json",
                json.getBytes(StandardCharsets.UTF_8)
        );
    }

}

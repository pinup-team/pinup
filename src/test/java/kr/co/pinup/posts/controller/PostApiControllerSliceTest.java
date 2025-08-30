package kr.co.pinup.posts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.ConstraintViolationException;
import kr.co.pinup.comments.repository.CommentRepository;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.Post;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.model.dto.UpdatePostRequest;
import kr.co.pinup.posts.repository.PostRepository;
import kr.co.pinup.posts.service.PostService;
import kr.co.pinup.security.filter.AccessTokenValidationFilter;
import kr.co.pinup.security.filter.SessionExpirationFilter;
import kr.co.pinup.stores.Store;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContext;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.io.IOException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostApiController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@Import({PostApiControllerSliceTest.TestConfig.class, PostApiControllerSliceTest.FilterBeansConfig.class, PostApiControllerSliceTest.TestSecurityConfig.class, LoggerConfig.class})
@AutoConfigureMockMvc(addFilters = true)
class PostApiControllerSliceTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Autowired private PostRepository postRepository;
    @Autowired private PostImageService postImageService;
    @Autowired private PostService postService;
    @RestControllerAdvice
    static class TestGlobalExceptionHandler {
        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
        }

        @ExceptionHandler(HandlerMethodValidationException.class)
        public ResponseEntity<String> handleValidation(HandlerMethodValidationException ex) {
            return ResponseEntity
                    .badRequest()
                    .body("요청 값이 잘못되었습니다: " + ex.getMessage());
        }

        @ExceptionHandler(ConstraintViolationException.class)
        public ResponseEntity<String> handleConstraintViolation(ConstraintViolationException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("요청 파라미터 오류: " + e.getMessage());
        }

        @ExceptionHandler(PostNotFoundException.class)
        public ResponseEntity<String> handlePostNotFound(PostNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }

    }

    @TestConfiguration
    static class TestConfig {

        @Bean public MemberService memberService() {
            MemberService mock = mock(MemberService.class);
            when(mock.isAccessTokenExpired(any(), any())).thenReturn(false);
            when(mock.refreshAccessToken(any())).thenReturn("mocked-token");
            return mock;
        }
        @Bean public PostService postService() {
            PostService mock = mock(PostService.class);

            when(mock.createPost(any(), any(), any()))
                    .thenReturn(new PostResponse(
                            1L,
                            "테스트유저",
                            "dummy title",  null,
                            LocalDateTime.now(),1L, 0,false
                    ));

            doAnswer(invocation -> {
                Long postId = invocation.getArgument(0);
                if (postId == 2L) {
                    throw new IllegalStateException("이미 비활성화된 게시글입니다.");
                }
                return null;
            }).when(mock).disablePost(any(Long.class));

            return mock;
        }
        @Bean public CommentService commentService() {return mock(CommentService.class);}
        @Bean public PostImageService postImageService()  {return mock(PostImageService.class);}

        @Bean public PostRepository postRepository() {return mock(PostRepository.class);}
        @Bean public CommentRepository commentRepository() {return mock(CommentRepository.class);}
        @Bean public MemberRepository memberRepository() {return mock(MemberRepository.class);}

    }

    @TestConfiguration
    static class FilterBeansConfig {

        @Bean
        public AccessTokenValidationFilter accessTokenValidationFilter() {
            return new AccessTokenValidationFilter(null, null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                        throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }

        @Bean
        public SessionExpirationFilter sessionExpirationFilter() {
            return new SessionExpirationFilter(null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
                        throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @EnableMethodSecurity
    @TestConfiguration
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/post/create", "/post/update/**").authenticated()
                            .anyRequest().permitAll()
                    )
                    .formLogin(withDefaults())
                    .build();
        }
    }

    @Target({ ElementType.METHOD, ElementType.TYPE })
    @Retention(RetentionPolicy.RUNTIME)
    @WithSecurityContext(factory = WithMockMemberSecurityContextFactory.class)
    public @interface WithMockMember {
        String nickname() default "테스트유저";
        OAuthProvider provider() default OAuthProvider.GOOGLE;
        MemberRole role() default MemberRole.ROLE_USER;
    }

    public static  class WithMockMemberSecurityContextFactory implements WithSecurityContextFactory<WithMockMember> {

        @Override
        public SecurityContext createSecurityContext(WithMockMember annotation) {
            MemberInfo principal = new MemberInfo(
                    annotation.nickname(),
                    annotation.provider(),
                    annotation.role()
            );

            String roleName = annotation.role().name();
            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleName));

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    principal,
                    "password",
                    authorities
            );

            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(auth);
            return context;
        }
    }


    @Nested
    @DisplayName("게시글 생성 - JSON 요청")
    class CreatePostJson {

        @Test
        @WithMockMember
        @DisplayName("게시글 생성 성공 - 이미지 2장 이상 포함")
        void createPost_success() throws Exception {
            CreatePostRequest request = new CreatePostRequest(1L, "제목", "내용");
            MockMultipartFile postPart = new MockMultipartFile(
                    "post", "post.json", "application/json", objectMapper.writeValueAsBytes(request));
            MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "image1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "image2".getBytes());

            mockMvc.perform(multipart("/api/post/create")
                            .file(postPart)
                            .file(image1)
                            .file(image2)
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockMember
        @DisplayName("유효성 실패 - 제목 없음")
        void createPost_validationFail_missingTitle() throws Exception {
            CreatePostRequest request = new CreatePostRequest(1L, "", "내용");
            MockMultipartFile postPart = new MockMultipartFile(
                    "post", "post.json", "application/json", objectMapper.writeValueAsBytes(request));
            MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "image1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "image2".getBytes());

            mockMvc.perform(multipart("/api/post/create")
                            .file(postPart)
                            .file(image1)
                            .file(image2)
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockMember
        @DisplayName("유효성 실패 - 내용 없음")
        void createPost_validationFail_missingContent() throws Exception {
            CreatePostRequest request = new CreatePostRequest(1L, "제목", "");
            MockMultipartFile postPart = new MockMultipartFile(
                    "post", "post.json", "application/json", objectMapper.writeValueAsBytes(request));
            MockMultipartFile image1 = new MockMultipartFile("images", "img1.jpg", "image/jpeg", "image1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "img2.jpg", "image/jpeg", "image2".getBytes());

            mockMvc.perform(multipart("/api/post/create")
                            .file(postPart)
                            .file(image1)
                            .file(image2)
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("게시글 생성 - Multipart 요청")
    class CreatePostMultipart {

        @Test
        @WithMockMember
        @DisplayName("파일 2장 이상 포함 시 생성 성공")
        void createPost_withValidMultipart_success() throws Exception {
            MockMultipartFile image1 = new MockMultipartFile("images", "file1.jpg", "image/jpeg", "data1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "file2.jpg", "image/jpeg", "data2".getBytes());

            String postJson = objectMapper.writeValueAsString(new CreatePostRequest(1L, "제목", "내용"));
            MockMultipartFile postPart = new MockMultipartFile(
                    "post", "post.json", "application/json", postJson.getBytes(StandardCharsets.UTF_8));

            mockMvc.perform(multipart(HttpMethod.POST, "/api/post/create")
                            .file(postPart)
                            .file(image1)
                            .file(image2)
                            .with(csrf())
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .accept(MediaType.APPLICATION_JSON))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockMember
        @DisplayName("이미지 1장만 포함 시 실패")
        void createPost_withSingleImage_validationFail() throws Exception {
            // Given: CreatePostRequest 객체 (JSON)와 이미지 1장을 포함한 요청
            CreatePostRequest postRequest = new CreatePostRequest(1L, "제목", "내용");
            MockMultipartFile postPart = new MockMultipartFile(
                    "post",
                    "post.json",
                    "application/json",
                    objectMapper.writeValueAsBytes(postRequest)
            );

            MockMultipartFile image1 = new MockMultipartFile(
                    "images",
                    "file1.jpg",
                    "image/jpeg",
                    "data1".getBytes()
            );

            // When: 게시글 생성 API 호출
            mockMvc.perform(multipart("/api/post/create")
                            .file(postPart)
                            .file(image1)
                            .with(csrf()))
                    // Then: 이미지 2장 미만이므로 400 오류
                    .andExpect(status().isBadRequest());
        }

    }

    @Nested
    @DisplayName("게시글 수정")
    class UpdatePost {

        @Test
        @WithMockMember
        @DisplayName("정상 수정 요청")
        void updatePost_success() throws Exception {
            // Given: 수정할 게시글 데이터와 이미지를 준비
            Store mockStore = Store.builder().name("Mock Store").build();
            Member mockMember = Member.builder().nickname("행복한돼지").build();

            Post existingPost = Post.builder()
                    .title("Original Title")
                    .content("Original Content")
                    .member(mockMember)
                    .store(mockStore)
                    .thumbnail("http://dummy-s3-url.com")
                    .build();
            ReflectionTestUtils.setField(existingPost, "id", 1L);

            when(postRepository.findById(1L)).thenReturn(Optional.of(existingPost));
            when(postRepository.save(any(Post.class))).thenReturn(existingPost);
            when(postImageService.findImagesByPostId(1L))
                    .thenReturn(Collections.singletonList(PostImageResponse.builder()
                            .s3Url("http://dummy-s3-url.com")
                            .build()));
            UpdatePostRequest updatePostRequest = new UpdatePostRequest("Updated Title", "Updated Content");
            String json = objectMapper.writeValueAsString(updatePostRequest);

            MockMultipartFile updatePostRequestPart = new MockMultipartFile(
                    "updatePostRequest", // 꼭 컨트롤러의 @RequestPart 이름과 일치해야 함
                    "updatePostRequest.json",
                    "application/json",
                    json.getBytes()
            );
            MockMultipartFile image1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "img1".getBytes());
            MockMultipartFile image2 = new MockMultipartFile("images", "image2.jpg", "image/jpeg", "img2".getBytes());

            // When: 게시글 수정 API 호출
            mockMvc.perform(MockMvcRequestBuilders.multipart("/api/post/{postId}", existingPost.getId())
                            .file(updatePostRequestPart)
                            .file(image1)
                            .file(image2)
                            .param("imagesToDelete", "")
                            .contentType(MediaType.MULTIPART_FORM_DATA)
                            .characterEncoding("utf-8")
                            .with(req -> { req.setMethod("PUT"); return req; }))
                    .andDo(print())
                    // Then: 응답 상태가 200 OK여야 한다
                    .andExpect(status().isOk());
        }

    }

    @Nested
    @DisplayName("게시글 단건 조회")
    class GetPostById {

        @Test
        @DisplayName("게시글 ID가 유효할 때 상세 조회에 성공한다")
        void getPostById_success() throws Exception {
            // Given: 게시글 조회를 위한 더미 데이터 준비
            Store mockStore = mock(Store.class);
            when(mockStore.getId()).thenReturn(1L);

            Member mockMember = mock(Member.class);
            when(mockMember.getId()).thenReturn(1L);

            Post dummyPost = Post.builder()
                    .title("제목")
                    .content("내용")
                    .store(mockStore)
                    .member(mockMember)
                    .build();

            ReflectionTestUtils.setField(dummyPost, "id", 1L);

            when(postRepository.findByIdAndIsDeleted(1L, false)).thenReturn(Optional.of(dummyPost));

            // When: 게시글 조회 API 호출
            mockMvc.perform(get("/api/post/1"))
                    // Then: 응답 상태가 200 OK여야 한다
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("게시글 ID가 음수일 경우 400 오류가 발생한다")
        void getPostById_invalidId() throws Exception {
            // Given: 유효하지 않은 게시글 ID
            // When: 게시글 조회 API 호출
            mockMvc.perform(get("/api/post/-1"))
                    // Then: 응답 상태가 400 Bad Request여야 한다
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("요청 파라미터 오류: getPostById.postId: must be greater than 0"));
        }
    }

    @Nested
    @DisplayName("게시글 전체 조회")
    class GetAllPosts {

        @Test
        @DisplayName("스토어 ID가 유효할 때 전체 조회에 성공한다")
        void getAllPosts_success() throws Exception {
            // Given: 정상적인 스토어 ID를 통한 게시글 조회
            // When: 게시글 목록 조회 API 호출
            mockMvc.perform(get("/api/post/list/1"))
                    // Then: 응답 상태가 200 OK여야 한다
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("스토어 ID가 음수일 경우 400 오류가 발생한다")
        void getAllPosts_invalidStoreId() throws Exception {
            // Given: 유효하지 않은 스토어 ID
            // When: 게시글 목록 조회 API 호출
            mockMvc.perform(get("/api/post/list/-1"))
                    // Then: 응답 상태가 400 Bad Request여야 한다
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("요청 파라미터 오류: getAllPosts.storeId: must be greater than 0"));

        }
    }

    @Nested
    @DisplayName("게시글 삭제")
    class DeletePost {

        @Test
        @WithMockMember(role = MemberRole.ROLE_ADMIN)
        @DisplayName("정상 삭제 요청")
        void deletePost_success() throws Exception {
            // Given: 삭제할 게시글 준비
            Post dummyPost = Post.builder()
                    .title("제목")
                    .content("내용")
                    .build();
            ReflectionTestUtils.setField(dummyPost, "id", 1L);

            when(postRepository.findById(1L)).thenReturn(Optional.of(dummyPost));
            doNothing().when(postRepository).delete(dummyPost);

            // When: 게시글 삭제 API 호출
            mockMvc.perform(delete("/api/post/1"))
                    // Then: 응답 상태가 204 No Content여야 한다
                    .andExpect(status().isNoContent());
        }
        @Test
        @WithMockMember
        @DisplayName("삭제 실패 - USER 권한으로 접근 시 403 Forbidden")
        void deletePost_forbiddenForUser() throws Exception {
            // Given: 게시글이 존재한다고 가정하더라도 USER는 권한 없음
            when(postRepository.findById(1L)).thenReturn(Optional.of(Post.builder()
                    .title("제목")
                    .content("내용")
                    .build()));

            // When: 일반 사용자가 게시글 삭제 요청
            mockMvc.perform(delete("/api/post/1"))
                    // Then: 403 Forbidden 발생
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockMember(role = MemberRole.ROLE_ADMIN)
        @DisplayName("삭제 실패 - 게시글 없음")
        void deletePost_notFound() throws Exception {
            // Given: PostService가 예외를 던지도록 설정
            doThrow(new PostNotFoundException("게시글을 찾을 수 없습니다."))
                    .when(postService).deletePost(9999L);

            // When: 게시글 삭제 API 호출
            mockMvc.perform(delete("/api/post/9999"))
                    // Then: 응답 상태가 404 Not Found여야 한다
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("게시글 비활성화")
    class DisablePost {

        @Test
        @WithMockMember
        @DisplayName("정상적인 게시글 ID일 경우 게시글을 비활성화한다")
        void disablePost_success() throws Exception {
            // Given: 정상적으로 비활성화할 수 있는 게시글 ID
            // When: 게시글 비활성화 요청 API 호출
            mockMvc.perform(patch("/api/post/1/disable"))
                    // Then: 응답 상태가 204 No Content여야 한다
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockMember
        @DisplayName("이미 비활성화된 게시글일 경우 409 오류가 발생한다")
        void disablePost_alreadyDisabled() throws Exception {
            // Given: 이미 비활성화된 게시글 ID
            Post post = Post.builder()
                    .title("제목")
                    .content("내용")
                    .isDeleted(true)  // 비활성화 상태로 설정
                    .build();
            ReflectionTestUtils.setField(post, "id", 2L);
            // Mock: 이미 비활성화된 게시글 반환
            when(postRepository.findById(2L)).thenReturn(Optional.of(post));
            mockMvc.perform(patch("/api/post/2/disable"))
                    // Then: 응답 상태가 409 Conflict여야 한다
                    .andExpect(status().isConflict())
                    .andExpect(content().string("이미 비활성화된 게시글입니다."));

        }
    }
}

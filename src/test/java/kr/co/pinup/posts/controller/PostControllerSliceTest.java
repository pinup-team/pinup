package kr.co.pinup.posts.controller;

import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.exception.ErrorResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.exception.post.PostNotFoundException;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.service.PostService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = PostController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@Import({PostControllerSliceTest.TestConfig.class, PostControllerSliceTest.TestSecurityConfig.class, PostControllerSliceTest.TestGlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = true)
class PostControllerSliceTest {

    private static final String VIEW_PREFIX = "views/posts/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PostService postService;

    @Autowired
    private CommentService commentService;

    @Autowired
    private PostImageService postImageService;


    @TestConfiguration
    static class TestConfig {
        @Bean PostService postService() { return mock(PostService.class); }
        @Bean CommentService commentService() { return mock(CommentService.class); }
        @Bean PostImageService postImageService() { return mock(PostImageService.class); }
    }

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

    @RestControllerAdvice
    static class TestGlobalExceptionHandler {

        @ExceptionHandler(PostNotFoundException.class)
        public ResponseEntity<ErrorResponse> handlePostNotFound(PostNotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.NOT_FOUND.value())
                            .message(ex.getMessage())
                            .build()
            );
        }

        @ExceptionHandler(HandlerMethodValidationException.class)
        public ResponseEntity<ErrorResponse> handleValidation(HandlerMethodValidationException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(
                    ErrorResponse.builder()
                            .status(HttpStatus.BAD_REQUEST.value())
                            .message("요청이 잘못되었습니다.")
                            .build()
            );
        }
    }


    @Nested
    @DisplayName("게시물 뷰 페이지 테스트")
    class ViewPageTests {

        @Test
        @DisplayName("게시물 리스트 페이지 이동 - 성공")
        void listPage_whenCalled_thenReturnsPostListView() throws Exception {
            Long storeId = 1L;
            when(postService.findByStoreIdWithCommentCount(storeId, false)).thenReturn(List.of());

            mockMvc.perform(get("/post/list/{storeId}", storeId))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW_PREFIX + "list"))
                    .andExpect(model().attributeExists("posts", "storeId"));
        }

        @Test
        @DisplayName("게시물 상세 페이지 이동 - 성공")
        void detailPage_whenExistingPost_thenReturnsPostDetailView() throws Exception {
            Long postId = 1L;
            PostResponse postResponse = mock(PostResponse.class);
            when(postService.getPostById(postId, false)).thenReturn(postResponse);
            when(commentService.findByPostId(postId)).thenReturn(List.of());
            when(postImageService.findImagesByPostId(postId)).thenReturn(List.of());

            mockMvc.perform(get("/post/{postId}", postId))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW_PREFIX + "detail"))
                    .andExpect(model().attributeExists("post", "comments", "images"));
        }

        @Test
        @DisplayName("존재하지 않는 게시물 ID로 상세 페이지 접근 시 404 에러 응답을 반환한다")
        void detailPage_whenPostNotFound_thenReturnsErrorResponse() throws Exception {
            Long invalidPostId = 999L;
            when(postService.getPostById(invalidPostId, false))
                    .thenThrow(new PostNotFoundException("게시물이 존재하지 않습니다."));

            mockMvc.perform(get("/post/{postId}", invalidPostId))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("게시물이 존재하지 않습니다.")));
        }


        @Test
        @DisplayName("음수 postId로 상세 페이지 요청 시 400 Bad Request 응답이 발생한다")
        void detailPage_whenNegativePostId_thenBadRequest() throws Exception {
            mockMvc.perform(get("/post/{postId}", -1))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("게시물 생성 뷰 페이지 테스트")
    class CreatePageTests {

        @Test
        @WithMockUser
        @DisplayName("게시물 생성 페이지 이동 - 성공")
        void createPage_whenAuthenticated_thenReturnsPostCreateView() throws Exception {
            mockMvc.perform(get("/post/create").param("storeId", "1"))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW_PREFIX + "create"))
                    .andExpect(model().attributeExists("storeId"));
        }

        @Test
        @WithMockUser
        @DisplayName("storeId가 음수일 경우 400 Bad Request 응답이 발생한다")
        void createPage_whenNegativeStoreId_thenBadRequest() throws Exception {
            mockMvc.perform(get("/post/create").param("storeId", "-1"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비로그인 사용자가 게시물 생성 페이지에 접근 시 로그인 페이지로 리다이렉트 된다")
        void createPage_whenNotAuthenticated_thenRedirectToLogin() throws Exception {
            mockMvc.perform(get("/post/create").param("storeId", "1"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

    }

    @Nested
    @DisplayName("게시물 수정 뷰 페이지 테스트")
    class UpdatePageTests {

        @Test
        @WithMockUser
        @DisplayName("게시물 수정 페이지 이동 - 성공")
        void updatePage_whenAuthenticated_thenReturnsPostUpdateView() throws Exception {
            Long postId = 1L;
            PostResponse postResponse = mock(PostResponse.class);
            when(postService.getPostById(postId, false)).thenReturn(postResponse);
            when(postImageService.findImagesByPostId(postId)).thenReturn(List.of());

            mockMvc.perform(get("/post/update/{postId}", postId))
                    .andExpect(status().isOk())
                    .andExpect(view().name(VIEW_PREFIX + "update"))
                    .andExpect(model().attributeExists("post", "images"));
        }

        @Test
        @WithMockUser
        @DisplayName("음수 postId로 게시물 수정 페이지 요청 시 400 Bad Request 응답이 발생한다")
        void updatePage_whenNegativePostId_thenBadRequest() throws Exception {
            mockMvc.perform(get("/post/update/{postId}", -10))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("비로그인 사용자가 게시물 수정 페이지에 접근 시 로그인 페이지로 리다이렉트 된다")
        void updatePage_whenNotAuthenticated_thenRedirectToLogin() throws Exception {
            Long postId = 1L;
            mockMvc.perform(get("/post/update/{postId}", postId))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/login"));
        }

    }
}
package kr.co.pinup.comments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.exception.GlobalExceptionHandler;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.security.filter.AccessTokenValidationFilter;
import kr.co.pinup.security.filter.SessionExpirationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.thymeleaf.ThymeleafAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = CommentApiController.class, excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@Import({
        GlobalExceptionHandler.class,
        CommentApiControllerSliceTest.TestConfig.class,
        CommentApiControllerSliceTest.TestSecurityConfig.class
})
@AutoConfigureMockMvc(addFilters = true)
class CommentApiControllerSliceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CommentService commentService;

    @TestConfiguration
    static class TestConfig {
        @Bean public CommentService commentService() { return mock(CommentService.class); }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(authz -> authz.anyRequest().permitAll())
                    .build();
        }

        @Bean public AccessTokenValidationFilter accessTokenValidationFilter() {
            return new AccessTokenValidationFilter(null, null) {
                @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(req, res);
                }
            };
        }

        @Bean public SessionExpirationFilter sessionExpirationFilter() {
            return new SessionExpirationFilter(null) {
                @Override protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(req, res);
                }
            };
        }
    }

    @Test
    @WithMockMember
    @DisplayName("댓글 생성 성공")
    void createComment_success() throws Exception {
        Long postId = 1L;
        CreateCommentRequest request = new CreateCommentRequest("테스트 댓글");

        when(commentService.createComment(any(MemberInfo.class), eq(postId), any()))
                .thenReturn(CommentResponse.builder().postId(postId).content("테스트 댓글").build());

        mockMvc.perform(post("/api/comment/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.postId").value(postId))
                .andExpect(jsonPath("$.content").value("테스트 댓글"));
    }

    @Test
    @WithMockMember
    @DisplayName("댓글 생성 실패 - content 없음")
    void createComment_fail_whenContentIsBlank() throws Exception {
        CreateCommentRequest invalid = new CreateCommentRequest("");

        mockMvc.perform(post("/api/comment/{postId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("내용을 입력해주세요.")));
    }

    @Test
    @WithMockMember
    @DisplayName("댓글 생성 실패 - postId가 음수")
    void createComment_fail_whenPostIdIsNegative() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("테스트 댓글");

        mockMvc.perform(post("/api/comment/{postId}", -1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("must be greater than 0")));
    }

    @Test
    @DisplayName("댓글 생성 실패 - 인증되지 않음")
    void createComment_fail_whenUnauthenticated() throws Exception {
        CreateCommentRequest request = new CreateCommentRequest("테스트 댓글");

        mockMvc.perform(post("/api/comment/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockMember(role = MemberRole.ROLE_ADMIN)
    @DisplayName("댓글 삭제 성공")
    void deleteComment_success() throws Exception {
        doNothing().when(commentService).deleteComment(1L);

        mockMvc.perform(delete("/api/comment/{commentId}", 1L))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("댓글 삭제 실패 - commentId가 음수")
    void deleteComment_fail_whenCommentIdIsNegative() throws Exception {
        mockMvc.perform(delete("/api/comment/{commentId}", -1L))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(containsString("must be greater than 0")));
    }
}

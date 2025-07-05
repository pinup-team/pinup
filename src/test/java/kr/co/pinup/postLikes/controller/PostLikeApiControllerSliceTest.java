package kr.co.pinup.postLikes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.exception.GlobalExceptionHandler;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.model.dto.PostLikeResponse;
import kr.co.pinup.postLikes.service.PostLikeService;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostLikeApiController.class,
        excludeAutoConfiguration = ThymeleafAutoConfiguration.class)
@Import({
        GlobalExceptionHandler.class,
        PostLikeApiControllerSliceTest.TestConfig.class,
        PostLikeApiControllerSliceTest.TestSecurityConfig.class,
        LoggerConfig.class
})
@AutoConfigureMockMvc(addFilters = true)
class PostLikeApiControllerSliceTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PostLikeService postLikeService;


    @TestConfiguration
    static class TestConfig {
        @Bean public PostLikeService postLikeService() {
            return mock(PostLikeService.class);
        }
    }

    @TestConfiguration
    static class TestSecurityConfig {
        @Bean public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }

        @Bean public AccessTokenValidationFilter accessTokenValidationFilter() {
            return new AccessTokenValidationFilter(null, null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(req, res);
                }
            };
        }

        @Bean public SessionExpirationFilter sessionExpirationFilter() {
            return new SessionExpirationFilter(null, null) {
                @Override
                protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(req, res);
                }
            };
        }
    }

    @Test
    @DisplayName("인증된 유저가 좋아요를 누르면 200과 JSON 반환")
    void toggleLike_authenticatedUser_shouldReturnResponse() throws Exception {
        // given
        MemberInfo memberInfo = new MemberInfo("행복한돼지", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        Authentication auth = new UsernamePasswordAuthenticationToken(memberInfo, "password", List.of());

        when(postLikeService.toggleLike(any(Long.class), any(MemberInfo.class)))
                .thenReturn(new PostLikeResponse(42, true));

        // when & then
        mockMvc.perform(post("/api/post-like/{postId}", 100L)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(auth)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.likedByCurrentUser").value(true))
                .andExpect(jsonPath("$.likeCount").value(42));
    }

}
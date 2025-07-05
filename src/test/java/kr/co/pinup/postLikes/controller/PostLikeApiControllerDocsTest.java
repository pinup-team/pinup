package kr.co.pinup.postLikes.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postLikes.model.dto.PostLikeResponse;
import kr.co.pinup.postLikes.service.PostLikeService;
import kr.co.pinup.support.RestDocsSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationResultHandler;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PostLikeApiController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({
        PostLikeApiControllerDocsTest.MockConfig.class,
        PostLikeApiControllerDocsTest.SecurityConfig.class,
        RestDocsSupport.class,
        LoggerConfig.class
})
class PostLikeApiControllerDocsTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PostLikeService postLikeService;
    @Autowired RestDocumentationResultHandler restDocs;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider provider) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean public PostLikeService postLikeService() {
            return mock(PostLikeService.class);
        }
    }

    @TestConfiguration
    static class SecurityConfig {
        @Bean public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            return http.csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("POST /api/post-like/{postId} - 게시글 좋아요 토글 문서화")
    void toggleLike_document() throws Exception {
        Long postId = 1L;

        given(postLikeService.toggleLike(eq(postId), any())).willReturn(
                new PostLikeResponse(15, true)
        );

        mockMvc.perform(post("/api/post-like/{postId}", postId)
                        .with(csrf())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(document("post-like-toggle",
                        pathParameters(
                                parameterWithName("postId").description("좋아요를 누를 게시글 ID")
                        ),
                        responseFields(
                                fieldWithPath("likeCount").description("총 좋아요 수"),
                                fieldWithPath("likedByCurrentUser").description("현재 사용자가 좋아요 눌렀는지 여부")
                        )
                ));
    }
}
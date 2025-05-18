package kr.co.pinup.comments.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.model.dto.CreateCommentRequest;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
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

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = CommentApiController.class)
@ExtendWith(RestDocumentationExtension.class)
@Import({CommentApiControllerDocsTest.MockConfig.class, CommentApiControllerDocsTest.SecurityConfig.class, RestDocsSupport.class})
class CommentApiControllerDocsTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired CommentService commentService;
    @Autowired RestDocumentationResultHandler restDocs;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider provider) {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(documentationConfiguration(provider))
                .alwaysDo(print())
                .alwaysDo(restDocs)
                .build();
    }

    @TestConfiguration
    static class MockConfig {
        @Bean public CommentService commentService() {
            return mock(CommentService.class);
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
    @DisplayName("POST /api/comment/{postId} - 댓글 생성 문서화")
    void createComment_document() throws Exception {
        Long postId = 1L;
        CreateCommentRequest request = new CreateCommentRequest("댓글 내용");
        String json = objectMapper.writeValueAsString(request);

        Member mockMember = Member.builder()
                .email("test@naver.com")
                .name("테스터")
                .nickname("행복한돼지")
                .providerType(OAuthProvider.NAVER)
                .providerId("naver123")
                .role(MemberRole.ROLE_USER)
                .build();

        CommentResponse response = new CommentResponse(
                1L, postId, mockMember, "댓글 내용", LocalDateTime.now());

        given(commentService.createComment(any(), eq(postId), any())).willReturn(response);

        mockMvc.perform(post("/api/comment/{postId}", postId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andDo(restDocs.document(
                        pathParameters(parameterWithName("postId").description("댓글을 작성할 게시글 ID")),
                        requestFields(fieldWithPath("content").description("댓글 내용")),
                        responseFields(
                                fieldWithPath("id").description("댓글 ID"),
                                fieldWithPath("postId").description("게시글 ID"),
                                fieldWithPath("content").description("댓글 내용"),
                                fieldWithPath("createdAt").description("작성 시각"),
                                fieldWithPath("member.id").optional().description("작성자 ID"),
                                fieldWithPath("member.nickname").description("작성자 닉네임"),
                                fieldWithPath("member.name").description("작성자 이름"),
                                fieldWithPath("member.email").description("작성자 이메일"),
                                fieldWithPath("member.providerType").description("소셜 로그인 제공자"),
                                fieldWithPath("member.providerId").description("소셜 제공자 식별자"),
                                fieldWithPath("member.role").description("작성자 권한"),
                                fieldWithPath("member.deleted").description("삭제 여부"),
                                fieldWithPath("member.createdAt").optional().description("작성자 생성 시각"),
                                fieldWithPath("member.updatedAt").optional().description("작성자 수정 시각")
                        )
                ));
    }

    @Test
    @WithMockMember(nickname = "행복한돼지", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("DELETE /api/comment/{commentId} - 댓글 삭제 문서화")
    void deleteComment_document() throws Exception {
        Long commentId = 1L;
        willDoNothing().given(commentService).deleteComment(commentId);

        mockMvc.perform(delete("/api/comment/{commentId}", commentId).with(csrf()))
                .andExpect(status().isNoContent())
                .andDo(restDocs.document(
                        pathParameters(parameterWithName("commentId").description("삭제할 댓글 ID"))
                ));
    }
}

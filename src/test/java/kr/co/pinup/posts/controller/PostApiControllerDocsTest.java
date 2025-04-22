package kr.co.pinup.posts.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.comments.model.dto.CommentResponse;
import kr.co.pinup.comments.service.CommentService;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.postImages.model.dto.PostImageResponse;
import kr.co.pinup.postImages.service.PostImageService;
import kr.co.pinup.posts.model.dto.CreatePostRequest;
import kr.co.pinup.posts.model.dto.PostDetailResponse;
import kr.co.pinup.posts.model.dto.PostResponse;
import kr.co.pinup.posts.service.PostService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import static io.restassured.RestAssured.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;

import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.multipart;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@WebMvcTest(PostApiController.class)
@AutoConfigureRestDocs(outputDir = "build/generated-snippets")
@Import({PostApiControllerDocsTest.MockConfig.class, PostApiControllerDocsTest.SecurityConfig.class})
class PostApiControllerDocsTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired PostService postService;
    @Autowired CommentService commentService;
    @Autowired PostImageService postImageService;

    @TestConfiguration
    static class MockConfig {
        @Bean public PostService postService() { return mock(PostService.class); }
        @Bean public CommentService commentService() { return mock(CommentService.class); }
        @Bean public PostImageService postImageService() { return mock(PostImageService.class); }
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
    @DisplayName("POST /api/post/create - 게시글 생성 문서화")
    void createPost_document() throws Exception {
        // given
        CreatePostRequest postRequest = new CreatePostRequest(1L, "문서화 제목", "문서화 내용");

        MockMultipartFile postPart = new MockMultipartFile(
                "post",
                "post.json",
                "application/json",
                objectMapper.writeValueAsBytes(postRequest)
        );

        MockMultipartFile image1 = new MockMultipartFile(
                "images",
                "image1.jpg",
                "image/jpeg",
                "img1".getBytes()
        );

        MockMultipartFile image2 = new MockMultipartFile(
                "images",
                "image2.jpg",
                "image/jpeg",
                "img2".getBytes()
        );

        // when + then
        mockMvc.perform(multipart("/api/post/create")
                        .file(postPart)
                        .file(image1)
                        .file(image2)
                        .with(csrf()))
                .andExpect(status().isCreated())
                .andDo(document("post-create",
                        requestParts(
                                partWithName("post").description("게시글 생성 정보 (storeId, title, content 포함)"),
                                partWithName("images").description("첨부 이미지 파일들 (최소 2장 이상)")
                        ),
                        requestPartFields("post",
                                fieldWithPath("storeId").description("스토어 ID"),
                                fieldWithPath("title").description("게시글 제목"),
                                fieldWithPath("content").description("게시글 내용")
                        )
                ));
    }


}

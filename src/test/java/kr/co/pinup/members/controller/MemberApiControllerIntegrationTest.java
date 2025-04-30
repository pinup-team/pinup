package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@Import(SecurityConfigTest.class)
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @Test
    @WithMockMember
    @DisplayName("회원 정보 수정 성공")
    void testUpdateMemberSuccessIntegration() throws Exception {
        MemberRequest request = createMemberRequest("updated-nickname");

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 정보 수정 실패")
    void testUpdateMemberFailureIntegration() throws Exception {
        MemberRequest request = createMemberRequest("incorrect-nickname");

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("닉네임 변경 실패"));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 탈퇴 성공")
    void testDeleteMemberSuccessIntegration() throws Exception {
        MemberRequest request = createMemberRequest("delete-nickname");

        mockMvc.perform(delete("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("탈퇴 성공"));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 탈퇴 실패")
    void testDeleteMemberFailureIntegration() throws Exception {
        // Arrange
        MemberRequest request = createMemberRequest("delete-nickname");

        // Act & Assert
        mockMvc.perform(delete("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("사용자 탈퇴 실패"));
    }

    @Test
    @WithMockMember
    @DisplayName("로그아웃 성공")
    void testLogoutSuccessIntegration() throws Exception {
        mockMvc.perform(post("/api/members/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }

    @Test
    @WithMockMember
    @DisplayName("로그아웃 실패")
    void testLogoutFailureIntegration() throws Exception {
        mockMvc.perform(post("/api/members/logout")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("로그아웃 실패"));
    }

    @Test
    @WithMockMember
    @DisplayName("닉네임 생성 성공")
    void testMakeNicknameSuccessIntegration() throws Exception {
        mockMvc.perform(get("/api/members/nickname")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("mock-nickname"));
    }

    @Test
    @DisplayName("닉네임 생성 실패 - 로그인 정보 없음")
    void testMakeNicknameFailureIntegration() throws Exception {
        mockMvc.perform(get("/api/members/nickname")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인 정보가 없습니다."));
    }

    private MemberRequest createMemberRequest(String nickname) {
        return MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname(nickname)
                .providerType(OAuthProvider.NAVER)
                .build();
    }
}
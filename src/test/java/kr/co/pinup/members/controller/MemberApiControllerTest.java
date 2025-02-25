package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(SecurityConfigTest.class)
@ExtendWith(SpringExtension.class)
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
    }

    @Test
    @WithMockMember
    @DisplayName("회원 정보 업데이트")
    void testUpdateMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();
        MemberResponse updatedMemberResponse = MemberResponse.builder().id(1L).name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();

        when(memberService.update(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(updatedMemberResponse);

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 탈퇴_성공")
    void testDeleteMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();

        when(memberService.delete(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

        mockMvc.perform(delete("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("탈퇴 성공"));
    }

    @Test
    @WithMockMember
    @DisplayName("로그아웃")
    void testLogout() throws Exception {
        when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(true);

        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }

    @Test
    @WithMockMember
    @DisplayName("토큰 정보 없는 로그아웃")
    void testLogoutWithoutLoginInfo() throws Exception {
        when(memberService.logout(any(OAuthProvider.class), any(String.class))).thenReturn(false);

        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("로그아웃 실패"));
    }
}
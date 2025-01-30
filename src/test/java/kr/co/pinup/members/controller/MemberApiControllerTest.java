package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private MemberApiController memberApiController;

    private ObjectMapper objectMapper;
    private MockHttpSession session;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;
    private MemberResponse updatedMemberResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberApiController).build();
        objectMapper = new ObjectMapper();

        session = Mockito.mock(MockHttpSession.class);
        member = Member.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("네이버TestMember")
                .providerType(OAuthProvider.NAVER)
                .providerId("123456789")
                .role(MemberRole.ROLE_USER)
                .build();
        memberInfo = MemberInfo.builder()
                .nickname("네이버TestMember")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
        memberRequest = MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("updatedTestNickname")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
        updatedMemberResponse = MemberResponse.builder()
                .id(1L)
                .name("test")
                .email("test@naver.com")
                .nickname("updatedTestNickname")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
        session.setAttribute("memberInfo", memberInfo);
    }

    @AfterEach
    void tearDown() {
        session.clearAttributes();
    }

    @Test
    @WithMockUser
    @DisplayName("회원 정보 업데이트")
    void testUpdateMember() throws Exception {
        when(memberService.update(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(updatedMemberResponse);

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .session(session)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andDo(print())
                .andExpect(status().isOk());
//                .andExpect(content().string("닉네임이 변경되었습니다."))
//                .andExpect(model().attribute("memberInfo", hasProperty("nickname", equalTo("updatedTestNickname"))));

//                .andExpect(jsonPath("$.code").exists())
//                .andExpect(jsonPath("$.message").exists())
//                .andExpect(jsonPath("$.code").value(200))
//                .andExpect(jsonPath("$.message").value("닉네임이 변경되었습니다."))
//                .andExpect(model().attribute("memberInfo", hasProperty("nickname", equalTo("updatedNickname")))); // 세션에 저장된 memberInfo 검증
    }

    @Test
    @WithMockUser
    @DisplayName("회원 탈퇴_성공")
    void testDeleteMember() throws Exception {
        MemberRequest memberRequest = new MemberRequest("test", "test@naver.com", "testNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);

        when(memberService.delete(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(true);

        mockMvc.perform(delete("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest))
                        .session(session))
                .andDo(print())
                .andExpect(status().isOk());
//                .andExpect(content().contentType(MediaType.TEXT_PLAIN));
//                .andExpect(content().string("탈퇴 성공"));
    }

    @Test
    @DisplayName("로그아웃")
    void testLogout() throws Exception {
        session.setAttribute("accessToken", "access-token-39349");
        when(memberService.logout(any(OAuthProvider.class), any(HttpServletRequest.class))).thenReturn(true);

        mockMvc.perform(post("/api/members/logout")
                        .session(session))
                .andExpect(status().isOk());
//                .andExpect(content().string("로그아웃 성공"));
    }
}
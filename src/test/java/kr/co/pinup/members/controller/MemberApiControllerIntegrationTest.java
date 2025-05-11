package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import jakarta.transaction.Transactional;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.google.GoogleResponse;
import kr.co.pinup.oauth.google.GoogleToken;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import kr.co.pinup.oauth.naver.NaverResponse;
import kr.co.pinup.oauth.naver.NaverToken;
import org.apache.commons.lang3.tuple.Triple;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")  // 테스트용 프로파일을 지정
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)  // 실제 웹환경에서 테스트, 실제 서버를 띄우지 않음
@AutoConfigureMockMvc  // MockMvc를 자동으로 설정해줌
@Transactional
public class MemberApiControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;  // MockMvc를 사용해 HTTP 요청을 시뮬레이션

    @Autowired
    private MemberService memberService;

    @Autowired
    private MemberRepository memberRepository;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        member = new Member("test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, "123456789", MemberRole.ROLE_USER, false);
        memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        memberRequest = new MemberRequest("test", "test@naver.com", "updatedTestNickname", OAuthProvider.NAVER);
        memberRepository.save(member);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, List.of(new SimpleGrantedAuthority(member.getRole().name())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    @Test
    @DisplayName("네이버 OAuth 로그인 성공")
    @Disabled
    void testLoginNaverSuccess() throws Exception {
        NaverLoginParams params = NaverLoginParams.builder().code("auth_code").state("1234567890").build(); // Mock or populate with necessary data
        NaverResponse oAuthResponse = NaverResponse.builder().response(NaverResponse.Response.builder().id("123456789").name("test").email("test@naver.com").build()).build(); // Mock or populate with necessary data
        NaverToken oAuthToken = NaverToken.builder().accessToken("access_token").refreshToken("refresh_token").build(); // Missing refresh token

        when(memberService.login(any(NaverLoginParams.class), any(HttpSession.class)))
                .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

        mockMvc.perform(get("/api/members/oauth/naver")
                        .param("code", "auth_code")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @DisplayName("구글 OAuth 로그인 성공")
    @Disabled
    void testLoginGoogleSuccess() throws Exception {
        GoogleLoginParams params = GoogleLoginParams.builder().code("auth_code").state("1234567890").build();
        GoogleResponse oAuthResponse = GoogleResponse.builder().sub("987654321").name("test").email("test@gmail.com").build();
        GoogleToken oAuthToken = GoogleToken.builder().accessToken("access_token").refreshToken("refresh_token").build();

        when(memberService.login(any(GoogleLoginParams.class), any(HttpSession.class)))
                .thenReturn(Triple.of(oAuthResponse, oAuthToken, "Success"));

        mockMvc.perform(get("/api/members/oauth/google")
                        .param("code", "auth_code")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "/"));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 닉네임 추천")
    void testMakeNickname() throws Exception {
        mockMvc.perform(get("/api/members/nickname")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.notNullValue()));
    }

    @Test
    @DisplayName("회원 정보 업데이트")
    @Disabled
    void testUpdateMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).build();

        mockMvc.perform(patch("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));

        Member updatedMember = memberRepository.findByEmailAndIsDeletedFalse(member.getEmail()).orElseThrow();
        assertEquals("updatedTestNickname", updatedMember.getNickname());
    }

    @Test
    @WithMockMember
    @DisplayName("로그아웃")
    @Disabled
    void testLogout() throws Exception {
        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }

    @Test
    @WithMockMember
    @DisplayName("토큰 정보 없는 로그아웃")
    @Disabled
    void testLogoutWithoutLoginInfo() throws Exception {
        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("로그아웃 실패"));
    }

    @Test
    @WithMockMember
    @DisplayName("회원 탈퇴_성공")
    @Disabled
    void testDeleteMember() throws Exception {
        MemberRequest memberRequest = MemberRequest.builder().name("test").email("test@naver.com").nickname("updatedTestNickname").providerType(OAuthProvider.NAVER).build();

        mockMvc.perform(delete("/api/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andDo(print())
                .andExpect(status().isOk());
//                .andExpect(content().string("탈퇴 성공"));

        Member disableMember = memberRepository.findByNickname(member.getNickname()).orElseThrow();
        assertTrue(disableMember.isDeleted());
    }
}

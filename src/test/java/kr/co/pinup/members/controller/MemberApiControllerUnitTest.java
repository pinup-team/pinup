package kr.co.pinup.members.controller;

import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MemberApiControllerUnitTest {

    private MemberApiController memberApiController;
    private MemberService memberService;
    private SecurityUtil securityUtil;

    private MemberInfo memberInfo;

    @BeforeEach
    void setUp() {
        memberService = mock(MemberService.class);
        securityUtil = mock(SecurityUtil.class);
        memberApiController = new MemberApiController(memberService, securityUtil);

        memberInfo = MemberInfo.builder()
                .nickname("네이버TestMember")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
    }

    @Test
    @DisplayName("회원 닉네임 추천")
    void testMakeNickname() {
        String generatedNickname = "mock-nickname";
        when(memberService.makeNickname()).thenReturn(generatedNickname);

        ResponseEntity<String> response = memberApiController.makeNickname(memberInfo);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(generatedNickname);
    }

    @Test
    @DisplayName("회원 정보 수정")
    void testUpdate() {
        MemberRequest request = MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("new-nick")
                .providerType(OAuthProvider.NAVER)
                .build();

        MemberResponse mockResponse = MemberResponse.builder()
                .id(1L)
                .name("test")
                .email("test@naver.com")
                .nickname("new-nick")
                .providerType(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .isDeleted(false)
                .build();

        when(memberService.update(memberInfo, request)).thenReturn(mockResponse);

        ResponseEntity<?> response = memberApiController.update(memberInfo, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("닉네임이 변경되었습니다.");
    }

    @Test
    @DisplayName("회원 정보 수정 실패 시 '닉네임 변경 실패' 반환")
    void testUpdateFailure() {
        MemberRequest request = MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("expected-nick")
                .providerType(OAuthProvider.KAKAO)
                .build();

        when(memberService.update(memberInfo, request)).thenReturn(
                MemberResponse.builder()
                        .nickname("mismatched-nick")
                        .build()
        );

        ResponseEntity<?> response = memberApiController.update(memberInfo, request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isEqualTo("닉네임 변경 실패");
    }

    @Test
    @DisplayName("회원 탈퇴 성공")
    void testDeleteMember() {
        MemberRequest request = MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("test-nick")
                .providerType(OAuthProvider.NAVER)
                .build();

        when(memberService.disable(memberInfo, request)).thenReturn(true);

        ResponseEntity<?> response = memberApiController.disable(memberInfo, request);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("탈퇴 성공");
    }

    @Test
    @DisplayName("회원 탈퇴 실패")
    void testDeleteMemberFailure() {
        MemberRequest request = MemberRequest.builder()
                .name("test")
                .email("test@naver.com")
                .nickname("test-nick")
                .providerType(OAuthProvider.KAKAO)
                .build();

        when(memberService.disable(memberInfo, request)).thenReturn(false);

        ResponseEntity<?> response = memberApiController.disable(memberInfo, request);

        assertThat(response.getStatusCode().is4xxClientError()).isTrue();
        assertThat(response.getBody()).isEqualTo("사용자 탈퇴 실패");
    }

    @Test
    @DisplayName("로그아웃 성공")
    void testLogout() {
        when(memberService.logout(memberInfo.provider(), "access-token")).thenReturn(true);
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("access-token");

        ResponseEntity<?> response = memberApiController.logout(memberInfo);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo("로그아웃 성공");
    }

    @Test
    @DisplayName("로그아웃 실패 - 토큰 정보 없음")
    void testLogoutFailure() {
        when(memberService.logout(memberInfo.provider(), "access-token")).thenReturn(false);
        when(securityUtil.getAccessTokenFromSecurityContext()).thenReturn("access-token");

        ResponseEntity<?> response = memberApiController.logout(memberInfo);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo("로그아웃 실패");
    }
}
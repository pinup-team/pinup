package kr.co.pinup.verification.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.MemberTestAnnotation;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.exception.MemberServiceException;
import kr.co.pinup.members.model.dto.*;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.security.SecurityUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MemberTestAnnotation
public class VerificationServiceUnitTest {
    MockMvc mockMvc;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SecurityUtil securityUtil;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private AppLogger appLogger;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;
    private MemberInfo mockTestInfo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberService).build();
        member = Member.builder()
                .name("test")
                .email("test@naver.com")
                .password(passwordEncoder.encode(""))
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
                .password("")
                .nickname("updatedTestNickname")
                .providerType(OAuthProvider.NAVER)
                .build();
        mockTestInfo = MemberInfo.builder()
                .nickname("mockNickname")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
    }

    @Nested
    @DisplayName("이메일 검증 테스트")
    class ValidateEmailTests {

        @Test
        @DisplayName("이메일이 null인 경우 false 반환")
        void testValidateEmail_WithNullEmail_ShouldReturnFalse() {
            boolean result = memberService.validateEmail(null);
            assertFalse(result);
        }

        @Test
        @DisplayName("이메일이 빈 문자열인 경우 false 반환")
        void testValidateEmail_WithEmptyEmail_ShouldReturnFalse() {
            boolean result = memberService.validateEmail("");
            assertFalse(result);
        }

        @Test
        @DisplayName("이미 존재하는 이메일인 경우 false 반환")
        void testValidateEmail_WithExistingEmail_ShouldReturnFalse() {
            String email = "test@naver.com";

            when(memberRepository.findByEmailAndIsDeletedFalse(email))
                    .thenReturn(Optional.of(member));

            boolean result = memberService.validateEmail(email);

            assertFalse(result);
        }

        @Test
        @DisplayName("신규 이메일인 경우 true 반환")
        void testValidateEmail_WithNewEmail_ShouldReturnTrue() {
            String email = "test@naver.com";

            when(memberRepository.findByEmailAndIsDeletedFalse(email))
                    .thenReturn(Optional.empty());

            boolean result = memberService.validateEmail(email);

            assertTrue(result);
        }
    }

    @Nested
    @DisplayName("회원 등록 관련 테스트")
    class RegisterMemberTests {

        @Test
        @DisplayName("회원 등록_정상")
        void testRegister_WithValidRequest_ShouldReturnNewMember() {
            // given
            MemberRequest pinupRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@test.com")
                    .password("test1234!")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.PINUP)
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(pinupRequest.email()))
                    .thenReturn(Optional.empty());

            // 닉네임 중복 검사
            when(memberRepository.findByNickname(pinupRequest.nickname()))
                    .thenReturn(Optional.empty());

            when(passwordEncoder.encode(anyString()))
                    .thenReturn("encoded-password");

            Member member = Member.builder()
                    .email(pinupRequest.email())
                    .nickname(pinupRequest.nickname())
                    .password("encoded-password")
                    .providerType(pinupRequest.providerType())
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(memberRepository.save(any(Member.class))).thenReturn(member);

            // when
            Pair<Member, String> result = memberService.register(pinupRequest);

            // then
            assertNotNull(result);
            assertEquals("updatedTestNickname", result.getLeft().getNickname());
        }

        @Test
        @DisplayName("회원 등록_이메일 누락")
        void testRegister_WithNullEmail_ShouldThrowBadRequest() {
            MemberRequest pinupRequest = MemberRequest.builder()
                    .name("test")
                    .email(null)
                    .password("test1234!")
                    .nickname("updatedTestNickname")
                    .providerType(OAuthProvider.PINUP)
                    .build();

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.register(pinupRequest);
            });

            assertEquals("이메일은 필수 입력 항목입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 등록_이메일 중복")
        void testRegister_WithDuplicateEmail_ShouldThrowBadRequest() {
            Member member1 = Member.builder()
                    .name("test")
                    .email("test@test.com")
                    .password(passwordEncoder.encode("test1234!"))
                    .nickname("핀업TestMember")
                    .providerType(OAuthProvider.PINUP)
                    .role(MemberRole.ROLE_USER)
                    .build();

            MemberRequest request = MemberRequest.builder()
                    .name("test")
                    .email("test@test.com")
                    .password("test1234!")
                    .nickname("핀업TestMember")
                    .providerType(OAuthProvider.PINUP)
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.of(member1));

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.register(request);
            });

            assertEquals("\"" + request.email() + "\"은 이미 가입된 이메일입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 등록_닉네임 누락")
        void testRegister_WithNullNickname_ShouldThrowBadRequest() {
            MemberRequest request = new MemberRequest(
                    "test",
                    "test@naver.com",
                    "password",
                    null,
                    OAuthProvider.PINUP
            );

            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.empty());

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.register(request);
            });

            assertEquals("닉네임은 필수 입력 항목입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 등록_닉네임 중복")
        void testRegister_WithDuplicateNickname_ShouldThrowBadRequest() {
            Member member1 = Member.builder()
                    .name("test")
                    .email("test@test.com")
                    .password(passwordEncoder.encode("test1234!"))
                    .nickname("duplicateNickname")
                    .providerType(OAuthProvider.PINUP)
                    .role(MemberRole.ROLE_USER)
                    .build();

            MemberRequest request = new MemberRequest(
                    "test",
                    "test@test.com",
                    "password",
                    "duplicateNickname",
                    OAuthProvider.PINUP
            );

            // 이메일은 중복 아님
            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.empty());

            // 닉네임 중복
            when(memberRepository.findByNickname(request.nickname()))
                    .thenReturn(Optional.of(member1));

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.register(request);
            });

            assertEquals("\"" + request.nickname() + "\"은 중복된 닉네임입니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("로그인 테스트")
    class LoginTests {

        @Mock
        private HttpSession session;

        MemberLoginRequest request = MemberLoginRequest.builder()
                .email("notfound@example.com")
                .password("test1234!")
                .providerType(OAuthProvider.PINUP).build();

        @Test
        @DisplayName("PINUP 로그인 시 비밀번호가 없으면 IllegalArgumentException 발생")
        void testLogin_PinupProvider_NoPassword_ShouldThrowIllegalArgumentException() {
            MemberNotFoundException ex = assertThrows(MemberNotFoundException.class, () -> {
                memberService.login(request, session);
            });

            assertEquals("사용자를 찾을 수 없습니다.\n이메일 혹은 비밀번호를 확인해주세요.", ex.getMessage());
        }

        @Test
        @DisplayName("존재하지 않는 이메일로 로그인 시 MemberNotFoundException 발생")
        void testLogin_EmailNotFound_ShouldThrowMemberNotFoundException() {
            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.empty());

            MemberNotFoundException ex = assertThrows(MemberNotFoundException.class, () -> {
                memberService.login(request, session);
            });

            assertEquals("사용자를 찾을 수 없습니다.\n이메일 혹은 비밀번호를 확인해주세요.", ex.getMessage());
        }

        @Test
        @DisplayName("다른 providerType으로 가입된 이메일 로그인 시 MemberServiceException 발생")
        void testLogin_DifferentProvider_ShouldThrowMemberServiceException() {
            Member member = Member.builder()
                    .email(request.email())
                    .providerType(OAuthProvider.NAVER)  // 다른 provider
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.of(member));

            MemberServiceException ex = assertThrows(MemberServiceException.class, () -> {
                memberService.login(request, session);
            });

            assertEquals("이 이메일은 네이버 로그인으로 가입된 계정입니다.", ex.getMessage());
        }

        @Test
        @DisplayName("비밀번호 불일치 시 UnauthorizedException 발생")
        void testLogin_PasswordMismatch_ShouldThrowUnauthorizedException() {
            Member member = Member.builder()
                    .email(request.email())
                    .providerType(OAuthProvider.PINUP)
                    .password(passwordEncoder.encode("test1234!"))
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.of(member));

            when(passwordEncoder.matches(request.password(), member.getPassword()))
                    .thenReturn(false);

            UnauthorizedException ex = assertThrows(UnauthorizedException.class, () -> {
                memberService.login(request, session);
            });

            assertEquals("비밀번호를 확인해주세요.", ex.getMessage());
        }

        @Test
        @DisplayName("정상 로그인 시 Member와 환영 메시지 반환")
        void testLogin_Success_ShouldReturnMemberAndMessage() {
            Member member = Member.builder()
                    .email(request.email())
                    .providerType(OAuthProvider.PINUP)
                    .password(passwordEncoder.encode("test1234!"))
                    .nickname("testNick")
                    .name("홍길동")
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(request.email()))
                    .thenReturn(Optional.of(member));
            when(passwordEncoder.matches(request.password(), member.getPassword()))
                    .thenReturn(true);

            // securityUtil.generateToken 모킹
            when(securityUtil.generateToken(member)).thenReturn("mockedAccessToken");

            // securityUtil.setAuthentication는 void 메서드이므로 doNothing() 사용 가능
            doNothing().when(securityUtil).setAuthentication(any(), any());

            Pair<Member, String> result = memberService.login(request, session);

            assertNotNull(result);
            assertEquals(member, result.getLeft());
            assertEquals("다시 돌아오신 걸 환영합니다 \"" + member.getName() + "\"님", result.getRight());

            verify(securityUtil).setAuthentication(any(), any());
            verify(session).setAttribute(eq(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY), any());
        }
    }

    @Nested
    @DisplayName("회원 조회 관련 테스트")
    class FindMemberTests {
        @Test
        @DisplayName("회원 조회_정상")
        public void testFindMember_WithValidNickname_ShouldReturnMemberResponse() {
            MemberResponse memberResponse = MemberResponse.builder()
                    .id(1L)
                    .name("test")
                    .email("test@naver.com")
                    .nickname("네이버TestMember")
                    .providerType(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));

            MemberResponse response = memberService.findMember(memberInfo);

            assertNotNull(response);
            assertEquals(memberResponse.getName(), response.getName());
            assertEquals(memberResponse.getEmail(), response.getEmail());
            assertEquals(memberResponse.getNickname(), response.getNickname());
            assertEquals(memberResponse.getProviderType(), response.getProviderType());
            assertEquals(memberResponse.getRole(), response.getRole());
            verify(memberRepository, times(1)).findByNickname(memberInfo.nickname());
        }

        @Test
        @DisplayName("회원 정보 조회 - 회원을 찾을 수 없을 경우")
        void testFindMember_WhenMemberNotFound_ShouldThrowMemberNotFoundException() {
            String nickname = "nonExistentNickname";

            when(memberRepository.findByNickname(nickname)).thenReturn(Optional.empty());

            assertThrows(MemberNotFoundException.class, () -> {
                memberService.findMember(MemberInfo.builder().nickname(nickname).provider(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build());
            });
        }
    }

    @Nested
    @DisplayName("회원 수정 관련 테스트")
    class UpdateMemberTests {
        @Test
        @DisplayName("회원 수정_정상")
        public void testUpdate_WithValidRequest_ShouldReturnUpdatedMember() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));

            when(memberRepository.findByNickname(memberRequest.nickname()))
                    .thenReturn(Optional.empty());

            when(memberRepository.save(any(Member.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            MemberResponse response = memberService.update(memberInfo, memberRequest);

            assertNotNull(response);
            assertEquals("updatedTestNickname", response.getNickname());
            verify(memberRepository).save(any(Member.class));
        }

        @Test
        @DisplayName("회원 수정_Email 일치하지 않음")
        public void testUpdate_WithEmailNotMatching_ShouldThrowMemberBadRequestException() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));
            MemberRequest testRequest = new MemberRequest(
                    "test",
                    "wrongEmail@example.com",
                    "",
                    "updatedTestNickname",
                    OAuthProvider.NAVER
            );

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.update(memberInfo, testRequest);
            });
            assertEquals("이메일이 일치하지 않습니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 수정_Nickname 길이 초과")
        public void testUpdate_WithNicknameIsTooLong_ShouldThrowMemberBadRequestException() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));
            MemberRequest testRequest = new MemberRequest(
                    "test",
                    "test@naver.com",
                    "",
                    "updatedTestNicknameupdatedTestNicknameupdatedTestNicknameupdatedTestNicknameupdatedTestNickname",
                    OAuthProvider.NAVER
            );

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.update(memberInfo, testRequest);
            });
            assertEquals("닉네임은 최대 50자입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 수정_Nickname 중복")
        public void testUpdate_WithDuplicateNickname_ShouldThrowMemberBadRequestException() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));
            when(memberRepository.findByNickname(memberRequest.nickname()))
                    .thenReturn(Optional.of(Member.builder()
                            .name("test")
                            .email("test@naver.com")
                            .nickname("updatedTestNickname")
                            .providerType(OAuthProvider.NAVER)
                            .providerId("123456789")
                            .role(MemberRole.ROLE_USER)
                            .build()));

            MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
                memberService.update(memberInfo, memberRequest);
            });
            assertEquals("\"updatedTestNickname\"은 중복된 닉네임입니다.", exception.getMessage());
        }

        @Test
        @DisplayName("회원 수정_저장 중 오류 발생")
        void testUpdate_WithExceptionInSave_ShouldThrowMemberServiceException() {
            // given
            when(memberRepository.findByNickname(eq(memberInfo.nickname())))
                    .thenReturn(Optional.of(member)); // 기존 회원 찾기

            when(memberRepository.findByNickname(memberRequest.nickname()))
                    .thenReturn(Optional.empty());

            doThrow(new RuntimeException("DB error"))
                    .when(memberRepository).save(any(Member.class)); // 저장 시 예외 발생

            // when & then
            MemberServiceException exception = assertThrows(MemberServiceException.class, () -> {
                memberService.update(memberInfo, memberRequest);
            });

            assertEquals("회원 정보 저장 중 오류가 발생했습니다.", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("회원 삭제 관련 테스트")
    class DeleteMemberTests {
        @Test
        @DisplayName("회원 삭제 - 정상")
        void testDeleteMember_WithValidEmail_ShouldReturnTrue() {
            when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));

            boolean result = memberService.disable(memberInfo, memberRequest);
            assertTrue(result);

            verify(memberRepository, times(1)).updateIsDeletedTrue(member.getId());
            verify(securityUtil).clearContextAndDeleteCookie();
        }

        @Test
        @DisplayName("회원 삭제 - 회원을 찾을 수 없을 경우")
        void testDeleteMember_WhenMemberNotFound_ShouldThrowMemberNotFoundException() {
            when(memberRepository.findByNickname(mockTestInfo.nickname())).thenReturn(Optional.empty());

            assertThrows(MemberNotFoundException.class, () -> {
                memberService.disable(new MemberInfo(mockTestInfo.nickname(), OAuthProvider.NAVER, MemberRole.ROLE_USER), memberRequest);
            });
        }

        @Test
        @DisplayName("회원 삭제 - 이메일 불일치")
        void testDeleteMember_WhenEmailNotMatch_ShouldThrowUnauthorizedException() {
            MemberRequest mockRequest = MemberRequest.builder()
                    .name("test")
                    .email("test@gmail.com")
                    .nickname("mockNickname")
                    .providerType(OAuthProvider.GOOGLE)
                    .build();

            when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));

            assertThrows(UnauthorizedException.class, () -> {
                memberService.disable(memberInfo, mockRequest);
            });
        }

        @Test
        @DisplayName("회원 삭제 - 삭제 중 오류 발생")
        void testDeleteMember_WhenDeleteFails_ShouldThrowMemberServiceException() {
            when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));
            doThrow(new RuntimeException("Database error")).when(memberRepository).updateIsDeletedTrue(any(Long.class));

            assertThrows(MemberServiceException.class, () -> {
                memberService.disable(memberInfo, memberRequest);
            });
        }
    }

    @Nested
    @DisplayName("비밀번호 재설정 테스트")
    class ResetPasswordTests {

        private MemberPasswordRequest passwordRequest;

        @BeforeEach
        void setUp() {
            passwordRequest = MemberPasswordRequest.builder()
                    .email("test@example.com")
                    .password("NewPass123!")
                    .providerType(OAuthProvider.PINUP)
                    .build();
        }

        @Test
        @DisplayName("회원 존재하지 않으면 MemberNotFoundException 발생")
        void testResetPassword_MemberNotFound() {
            when(memberRepository.findByEmailAndIsDeletedFalse(passwordRequest.email()))
                    .thenReturn(Optional.empty());

            MemberNotFoundException ex = assertThrows(MemberNotFoundException.class, () -> {
                memberService.resetPassword(passwordRequest);
            });

            assertEquals("사용자를 찾을 수 없습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("providerType 불일치 시 MemberBadRequestException 발생")
        void testResetPassword_ProviderMismatch() {
            Member member = Member.builder()
                    .email(passwordRequest.email())
                    .providerType(OAuthProvider.NAVER)
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(passwordRequest.email()))
                    .thenReturn(Optional.of(member));

            MemberBadRequestException ex = assertThrows(MemberBadRequestException.class, () -> {
                memberService.resetPassword(passwordRequest);
            });

            assertEquals("가입경로가 일치하지 않습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("비밀번호 재설정 성공")
        void testResetPassword_Success() {
            Member member = Member.builder()
                    .email(passwordRequest.email())
                    .providerType(OAuthProvider.PINUP)
                    .password("oldPassword")
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(passwordRequest.email()))
                    .thenReturn(Optional.of(member));
            when(passwordEncoder.encode(passwordRequest.password()))
                    .thenReturn("encodedNewPassword");
            when(memberRepository.save(any(Member.class))).thenAnswer(invocation -> invocation.getArgument(0));

            MemberResponse response = memberService.resetPassword(passwordRequest);

            assertNotNull(response);
            assertEquals(passwordRequest.email(), response.getEmail());
            verify(memberRepository).save(member);
            assertEquals("encodedNewPassword", member.getPassword());
        }

        @Test
        @DisplayName("DataIntegrityViolationException 발생 시 MemberServiceException 발생")
        void testResetPassword_DataIntegrityViolationException() {
            Member member = Member.builder()
                    .email(passwordRequest.email())
                    .providerType(OAuthProvider.PINUP)
                    .password("oldPassword")
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(passwordRequest.email()))
                    .thenReturn(Optional.of(member));
            when(passwordEncoder.encode(passwordRequest.password()))
                    .thenReturn("encodedNewPassword");
            when(memberRepository.save(any(Member.class)))
                    .thenThrow(new DataIntegrityViolationException("constraint violation"));

            MemberServiceException ex = assertThrows(MemberServiceException.class, () -> {
                memberService.resetPassword(passwordRequest);
            });

            assertEquals("회원 비밀번호 변경 중 제약 조건 위반이 발생했습니다.", ex.getMessage());
        }

        @Test
        @DisplayName("기타 예외 발생 시 MemberServiceException 발생")
        void testResetPassword_OtherException() {
            Member member = Member.builder()
                    .email(passwordRequest.email())
                    .providerType(OAuthProvider.PINUP)
                    .password("oldPassword")
                    .build();

            when(memberRepository.findByEmailAndIsDeletedFalse(passwordRequest.email()))
                    .thenReturn(Optional.of(member));
            when(passwordEncoder.encode(passwordRequest.password()))
                    .thenReturn("encodedNewPassword");
            when(memberRepository.save(any(Member.class)))
                    .thenThrow(new RuntimeException("unknown error"));

            MemberServiceException ex = assertThrows(MemberServiceException.class, () -> {
                memberService.resetPassword(passwordRequest);
            });

            assertEquals("회원 비밀번호 변경 중 오류가 발생했습니다.", ex.getMessage());
        }
    }
}
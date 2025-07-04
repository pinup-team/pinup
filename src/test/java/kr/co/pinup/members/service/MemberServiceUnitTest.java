package kr.co.pinup.members.service;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.MemberTestAnnotation;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.exception.MemberServiceException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MemberTestAnnotation
public class MemberServiceUnitTest {
    MockMvc mockMvc;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SecurityUtil securityUtil;

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
                .build();
        mockTestInfo = MemberInfo.builder()
                .nickname("mockNickname")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
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
            verify(memberRepository).save(member);
        }

        @Test
        @DisplayName("회원 수정_Email 일치하지 않음")
        public void testUpdate_WithEmailNotMatching_ShouldThrowMemberBadRequestException() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));
            MemberRequest testRequest = new MemberRequest(
                    "test",
                    "wrongEmail@example.com",
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
        public void testUpdate_WithExceptionInSave_ShouldThrowMemberServiceException() {
            when(memberRepository.findByNickname(memberInfo.nickname()))
                    .thenReturn(Optional.of(member));
            when(memberRepository.findByNickname(memberRequest.nickname()))
                    .thenReturn(Optional.empty());
            doThrow(new RuntimeException("Database error")).when(memberRepository).save(member);

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
}
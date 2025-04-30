package kr.co.pinup.members.service;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.custom.MemberTestAnnotation;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.security.SecurityUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@MemberTestAnnotation
class MemberServiceUnitTest {
    MockMvc mockMvc;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private SecurityUtil securityUtil;

    @InjectMocks
    private MemberService memberService;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberService).build();
        member = new Member("test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, "123456789", MemberRole.ROLE_USER, false);
        memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        memberRequest = new MemberRequest("test", "test@naver.com", "updatedTestNickname", OAuthProvider.NAVER);
    }

    @Test
    @DisplayName("회원 조회_정상")
    void testFindMember_WithValidNickname_ShouldReturnMemberResponse() {
        // Arrange
        when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));

        // Act
        MemberResponse response = memberService.findMember(memberInfo);

        // Assert
        assertNotNull(response);
        assertEquals("test", response.getName());
        assertEquals("test@naver.com", response.getEmail());
        assertEquals("네이버TestMember", response.getNickname());
        assertEquals(OAuthProvider.NAVER, response.getProviderType());
        assertEquals(MemberRole.ROLE_USER, response.getRole());
        verify(memberRepository).findByNickname(memberInfo.nickname());
    }

    @Test
    @DisplayName("회원 조회_회원이 없을 경우")
    void testFindMember_WhenMemberNotFound_ShouldThrowMemberNotFoundException() {
        // Arrange: Mock the repository's behavior
        when(memberRepository.findByNickname("nonExistentNickname")).thenReturn(Optional.empty());

        // Act and Assert: Verify that calling memberService.findMember() throws MemberNotFoundException
        assertThrows(MemberNotFoundException.class, () -> memberService.findMember(memberInfo));
    }

    @Test
    @DisplayName("회원 수정_정상")
    void testUpdate_WithValidRequest_ShouldReturnUpdatedMember() {
        // Arrange
        when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(memberRequest.nickname())).thenReturn(Optional.empty());
        when(memberRepository.save(any(Member.class))).thenReturn(member);

        // Act
        MemberResponse response = memberService.update(memberInfo, memberRequest);

        // Assert
        assertNotNull(response);
        assertEquals("updatedTestNickname", response.getNickname());
        verify(memberRepository).save(any(Member.class));
    }

    @Test
    @DisplayName("회원 수정_이메일 불일치")
    void testUpdate_WithEmailNotMatching_ShouldThrowMemberBadRequestException() {
        // Arrange
        MemberRequest invalidRequest = new MemberRequest("test", "wrongEmail@example.com", "updatedTestNickname", OAuthProvider.NAVER);

        when(memberRepository.findByNickname(memberInfo.nickname())).thenReturn(Optional.of(member));

        // Act and Assert
        assertThrows(MemberBadRequestException.class, () -> memberService.update(memberInfo, invalidRequest));
    }
}

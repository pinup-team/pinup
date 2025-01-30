package kr.co.pinup.members.service;

import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberBadRequestException;
import kr.co.pinup.members.exception.MemberServiceException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@kr.co.pinup.members.annotation.MemberServiceTest
public class MemberServiceTest {
    MockMvc mockMvc;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @InjectMocks
    private static JdbcTemplate jdbcTemplate;

    private Member member;
    private MemberInfo memberInfo;
    private MemberRequest memberRequest;
    private MemberInfo mockTestInfo;

    @BeforeEach
    void setUp() {
        // H2 데이터베이스 설정
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

        // members 테이블 생성
        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS members (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "name VARCHAR(50) NOT NULL," +
                "email VARCHAR(100) NOT NULL UNIQUE," +
                "nickname VARCHAR(50) NOT NULL UNIQUE," +
                "provider_type VARCHAR(50) NOT NULL," +
                "provider_id VARCHAR(255) NOT NULL," +
                "role VARCHAR(50) NOT NULL," +
                "created_at TIMESTAMP," +
                "updated_at TIMESTAMP" +
                ")");

        // 데이터 삽입
        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
                "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER.toString(), "123456789", MemberRole.ROLE_USER.toString());

        // MockMvc 설정
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
                .role(MemberRole.ROLE_USER)
                .build();
        mockTestInfo = MemberInfo.builder()
                .nickname("mockNickname")
                .provider(OAuthProvider.NAVER)
                .role(MemberRole.ROLE_USER)
                .build();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM members");
    }

    @Test
    @DisplayName("회원 수정_정상")
    public void testUpdate_WithValidRequest_ShouldReturnUpdatedMember() {
        when(memberRepository.findByNickname(memberInfo.nickname()))
                .thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(memberRequest.nickname()))
                .thenReturn(Optional.empty());

        // memberRepository.save() 이후 savedMember 객체가 저장되도록 설정
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
                OAuthProvider.NAVER,
                MemberRole.ROLE_USER
        );

        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, testRequest);
        });
        assertEquals("이메일이 일치하지 않습니다.", exception.getMessage());
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
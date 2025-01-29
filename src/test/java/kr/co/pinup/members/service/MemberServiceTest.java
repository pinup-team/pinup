package kr.co.pinup.members.service;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@kr.co.pinup.members.annotation.MemberServiceTest
public class MemberServiceTest {
    MockMvc mockMvc;

//    @MockitoBean
//    MemberController memberController;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private MemberService memberService;

    @InjectMocks
    private static JdbcTemplate jdbcTemplate;

    private MockHttpSession session;
    private ObjectMapper objectMapper;

    private Member member = Member.builder()
            .name("test")
            .email("test@naver.com")
            .nickname("네이버TestMember")
            .providerType(OAuthProvider.NAVER)
            .providerId("123456789")
            .role(MemberRole.ROLE_USER)
            .build();
    private MemberInfo memberInfo = new MemberInfo(
            "testMember",
            OAuthProvider.NAVER,
            MemberRole.ROLE_USER
    );
    private MemberRequest memberRequest = new MemberRequest(
            "test",
            "test@naver.com",
            "updatedTestNickname",
            OAuthProvider.NAVER,
            MemberRole.ROLE_USER
    );
    private MemberInfo mockTestInfo = new MemberInfo(
            "mockNickname",
            OAuthProvider.NAVER,
            MemberRole.ROLE_USER
    );

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
        session = new MockHttpSession();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM members");
        session.clearAttributes();
    }

    @Test
    @DisplayName("MemberLogin 검증")
    public void testLogin() {
        when(memberRepository.findByEmail(member.getEmail())).thenReturn(Optional.ofNullable(member));

    }

    @Test
    public void testUpdate_WithValidRequest_ShouldReturnUpdatedMember() {
        // given
        when(memberRepository.findByNickname(memberInfo.getNickname()))
                .thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(memberRequest.getNickname()))
                .thenReturn(Optional.empty());

        // when
        MemberResponse response = memberService.update(memberInfo, memberRequest);

        // then
        assertNotNull(response);
        assertEquals("newNickname", response.getNickname());
        verify(memberRepository).save(member);
    }

    @Test
    public void testUpdate_WithNullMemberRequest_ShouldThrowMemberBadRequestException() {
        // given
        memberRequest = null;

        // when & then
        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, memberRequest);
        });
        assertEquals("회원 요청 정보가 누락되었습니다.", exception.getMessage());
    }

    @Test
    public void testUpdate_WithNullMemberInfo_ShouldThrowMemberBadRequestException() {
        // given
        memberInfo = null;

        // when & then
        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, memberRequest);
        });
        assertEquals("회원 정보가 누락되었습니다.", exception.getMessage());
    }

    @Test
    public void testUpdate_WithNullEmail_ShouldThrowMemberBadRequestException() {
        MemberRequest testRequest = new MemberRequest(
                "test",
                null,
                "updatedTestNickname",
                OAuthProvider.NAVER,
                MemberRole.ROLE_USER
        );

        // when & then
        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, testRequest);
        });
        assertEquals("이메일은 null일 수 없습니다.", exception.getMessage());
    }

    @Test
    public void testUpdate_WithEmailNotMatching_ShouldThrowMemberBadRequestException() {
        // given
        when(memberRepository.findByNickname(memberInfo.getNickname()))
                .thenReturn(Optional.of(member));
        MemberRequest testRequest = new MemberRequest(
                "test",
                "wrongEmail@example.com",
                "updatedTestNickname",
                OAuthProvider.NAVER,
                MemberRole.ROLE_USER
        );

        // when & then
        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, testRequest);
        });
        assertEquals("이메일이 일치하지 않습니다.", exception.getMessage());
    }

    @Test
    public void testUpdate_WithDuplicateNickname_ShouldThrowMemberBadRequestException() {
        // given
        when(memberRepository.findByNickname(memberInfo.getNickname()))
                .thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(memberRequest.getNickname()))
                .thenReturn(Optional.of(Member.builder()
                        .name("test")
                        .email("otherEmail@example.com")
                        .nickname("duplicateNickname")
                        .providerType(OAuthProvider.NAVER)
                        .providerId("123456789")
                        .role(MemberRole.ROLE_USER)
                        .build()));

        // when & then
        MemberBadRequestException exception = assertThrows(MemberBadRequestException.class, () -> {
            memberService.update(memberInfo, memberRequest);
        });
        assertEquals("\"newNickname\"은 중복된 닉네임입니다.", exception.getMessage());
    }

    @Test
    public void testUpdate_WithExceptionInSave_ShouldThrowMemberServiceException() {
        // given
        when(memberRepository.findByNickname(memberInfo.getNickname()))
                .thenReturn(Optional.of(member));
        when(memberRepository.findByNickname(memberRequest.getNickname()))
                .thenReturn(Optional.empty());
        doThrow(new RuntimeException("Database error")).when(memberRepository).save(member);

        // when & then
        MemberServiceException exception = assertThrows(MemberServiceException.class, () -> {
            memberService.update(memberInfo, memberRequest);
        });
        assertEquals("회원 정보 저장 중 오류가 발생했습니다.", exception.getMessage());
    }
}
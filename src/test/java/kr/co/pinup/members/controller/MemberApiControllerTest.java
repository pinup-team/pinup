package kr.co.pinup.members.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MemberApiController.class)
public class MemberApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberApiController memberApiController;

    private MockHttpSession session;
    private MemberInfo testMemberInfo;

    @Autowired
    private static JdbcTemplate jdbcTemplate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(memberApiController).build();
        session = new MockHttpSession();
        testMemberInfo = MemberInfo.builder().nickname("testNickname").provider(OAuthProvider.NAVER).role(MemberRole.ROLE_USER).build();
        session.setAttribute("memberInfo", testMemberInfo);
        objectMapper = new ObjectMapper();

        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

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

        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
                "test", "test@naver.com", "testNickname", OAuthProvider.NAVER.toString(), "123456789", MemberRole.ROLE_USER.toString());
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM members");
        session.clearAttributes();
    }

    @Test
    @DisplayName("회원 정보 업데이트")
    void testUpdateMember() throws Exception {
        MemberRequest memberRequest = new MemberRequest("test", "test@naver.com", "updatedNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        MemberResponse updatedMemberResponse = new MemberResponse(1L, "test", "test@naver.com", "updatedNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        when(memberService.update(any(MemberInfo.class), any(MemberRequest.class))).thenReturn(updatedMemberResponse);

        mockMvc.perform(patch("/api/members")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberRequest)))
                .andExpect(status().isOk())
                .andExpect(content().string("닉네임이 변경되었습니다."));

        // memberService.update()가 한 번 호출되었는지 검증
        verify(memberService, times(1)).update(any(MemberInfo.class), any(MemberRequest.class));

        // 세션이 업데이트되었는지 검증
        verify(session, times(1)).setAttribute(eq("memberInfo"), any(MemberInfo.class));
    }

    @Test
    @DisplayName("회원 탈퇴")
    void testDeleteMember() throws Exception {
        MemberRequest memberRequest = new MemberRequest("test", "test@naver.com", "testNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        when(memberService.delete(any(), any())).thenReturn(true);

        mockMvc.perform(delete("/api/members")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(memberRequest))
                        .sessionAttr("memberInfo", testMemberInfo))
                .andExpect(status().isOk())
                .andExpect(content().string("탈퇴 성공"));
    }

    @Test
    @DisplayName("로그아웃")
    void testLogout() throws Exception {
        when(memberService.logout(any(), any())).thenReturn(true);

        mockMvc.perform(post("/api/members/logout")
                        .sessionAttr("memberInfo", testMemberInfo))
                .andExpect(status().isOk())
                .andExpect(content().string("로그아웃 성공"));
    }

    @Test
    @DisplayName("로그인 정보 없는 로그아웃")
    void testLogoutWithoutLoginInfo() throws Exception {
        mockMvc.perform(post("/api/members/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string("로그인 정보가 없습니다."));
    }
}
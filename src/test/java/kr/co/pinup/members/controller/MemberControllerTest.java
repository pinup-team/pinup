package kr.co.pinup.members.controller;

import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@ExtendWith(SpringExtension.class)
@WebMvcTest(MemberController.class)
public class MemberControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    MemberController memberController;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    private MockHttpSession session;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("CREATE TABLE IF NOT EXISTS members (" + "id BIGINT AUTO_INCREMENT PRIMARY KEY," + "name VARCHAR(50) NOT NULL," + "email VARCHAR(100) NOT NULL UNIQUE," + "nickname VARCHAR(50) NOT NULL UNIQUE," + "provider_type VARCHAR(50) NOT NULL," + "provider_id VARCHAR(255) NOT NULL," + "role VARCHAR(50) NOT NULL," + "created_at TIMESTAMP," + "updated_at TIMESTAMP" + ")");
        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)", "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER.toString(), "123456789", MemberRole.ROLE_USER.toString());

        mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
        session = new MockHttpSession();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM members");
        session.clearAttributes();
    }

    @Test
    @DisplayName("Login 페이지 이동")
    void login() throws Exception {
        String viewName = mockMvc.perform(get("/members/login")).andReturn().getModelAndView().getViewName();

        assertEquals("members/login", viewName);
    }

    @Test
    @DisplayName("마이 페이지 이동")
    void memberProfile() throws Exception {
        MemberInfo memberInfo = new MemberInfo("네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);
        MemberResponse testResponse = new MemberResponse(1L, "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER);

        when(memberService.findMember(any(MemberInfo.class))).thenReturn(testResponse);
        session.setAttribute("memberInfo", memberInfo);

        String viewName = mockMvc.perform(get("/members/profile").session(session))
                .andReturn()
                .getModelAndView()
                .getViewName();

        assertEquals("members/profile", viewName);
    }
}

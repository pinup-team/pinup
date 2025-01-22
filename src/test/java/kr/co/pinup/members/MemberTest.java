package kr.co.pinup.members;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.PinupApplication;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.controller.MemberController;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = PinupApplication.class)
public class MemberTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    @Autowired
    private static JdbcTemplate jdbcTemplate;

    private MockHttpSession session;
    private ObjectMapper objectMapper;

    private MemberInfo testInfo = new MemberInfo("네이버TestUser", OAuthProvider.NAVER, MemberRole.ROLE_USER);
    private MemberRequest testRequest = new MemberRequest("test", "test@naver.com", "네이버TestUser", OAuthProvider.NAVER, MemberRole.ROLE_USER);
    private MemberResponse testResponse = new MemberResponse(1L, "test", "test@naver.com", "네이버TestUser", OAuthProvider.NAVER, MemberRole.ROLE_USER);

    private MemberInfo mockTestInfo = new MemberInfo("mockNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
    private MemberRequest mockTestRequest = new MemberRequest("mock", "mock@naver.com", "mockNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);
    private MemberResponse mockTestResponse = new MemberResponse(1L, "mock", "mock@naver.com", "mockNickname", OAuthProvider.NAVER, MemberRole.ROLE_USER);

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
                "test", "test@naver.com", "네이버TestUser", "NAVER", "123456789", "ROLE_USER");

        mockMvc = MockMvcBuilders.standaloneSetup(memberController).build();
        session = new MockHttpSession();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM members");
        session.clearAttributes();
    }

    @Test
    void testLogin() {
        // Given
        String expectedView = "users/login";

        // When
        String view = memberController.login();

        // Then
        assertEquals(expectedView, view);
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class findUser {
        @Test
        void 조회_성공() throws Exception { // CHECK
            given(memberService.findUser(testInfo)).willReturn(testResponse);

            MockHttpSession session = new MockHttpSession();

            mockMvc.perform(get("/users/profile")
                            .session(session)
                            .sessionAttr("userInfo", testInfo))
                    .andExpect(status().isOk())
//                    .andExpect(view().name("error"))
                    .andExpect(view().name("users/profile"))
                    .andExpect(model().attribute("profile", testResponse))
                    .andDo(print());
        }

        @Test
        void 조회_사용자_없음() throws Exception {
            MemberInfo memberInfo = MemberInfo.builder()
                    .nickname("고독한치타")
                    .provider(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();

            given(memberService.findUser(memberInfo)).willReturn(null);

            MockHttpSession session = new MockHttpSession();
            session.setAttribute("userInfo", memberInfo);

            mockMvc.perform(get("/users/profile")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "사용자를 찾을 수 없습니다."))
                    .andDo(print());
        }

        @Test
        void 조회_UserInfo없음() throws Exception { // CHECK
            mockMvc.perform(get("/users/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "로그인 정보가 없습니다."))
                    .andDo(print());
        }

        @Test
        void 조회_UserInfo있음_유저정보없음() throws Exception {
            given(memberService.findUser(mockTestInfo)).willReturn(null);

            mockMvc.perform(get("/users/profile")
                            .sessionAttr("userInfo", mockTestInfo))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "사용자를 찾을 수 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("유저 수정 테스트") // CHECK
    class updateUser {
        @Test
        void 수정_성공() throws Exception {
            given(memberService.update(testInfo, testRequest)).willReturn(testResponse);

            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(testResponse)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("닉네임이 변경되었습니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_중복닉네임() throws Exception {
            MemberRequest memberRequest = new MemberRequest("test", "test@naver.com", "조용한고래", OAuthProvider.NAVER, MemberRole.ROLE_USER);

            given(memberService.update(testInfo, memberRequest)).willThrow(new IllegalArgumentException("\"" + memberRequest.getNickname() + "\"은 중복된 닉네임입니다."));
//            given(userService.update(mockUserInfo, memberDto)).willReturn(memberDto);

            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("중복된 닉네임입니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_UserInfo없음() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", eq(null))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("유저 탈퇴 테스트") // CHECK
    class deleteUser {
        @Test
        void 탈퇴_성공() throws Exception {
            given(memberService.delete(testInfo, testRequest)).willReturn(true);

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("탈퇴 성공"))
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패_UserNotFound() throws Exception {
            given(memberService.delete(mockTestInfo, mockTestRequest)).willThrow(new MemberNotFoundException("사용자를 찾을 수 없습니다."));

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", mockTestInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(mockTestRequest)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("사용자를 찾을 수 없습니다."))
                    .andDo(print());

            ArgumentCaptor<MemberRequest> captor = ArgumentCaptor.forClass(MemberRequest.class);
            verify(memberService).delete(eq(testInfo), captor.capture());
            assertEquals(mockTestResponse, captor.getValue());
        }

        @Test
        void 탈퇴_실패_EmailNotEqual() throws Exception {
            given(memberService.delete(testInfo, mockTestRequest)).willThrow(new UnauthorizedException("권한이 없습니다."));

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(mockTestRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("권한이 없습니다."))
                    .andDo(print());

            ArgumentCaptor<MemberRequest> captor = ArgumentCaptor.forClass(MemberRequest.class);
            verify(memberService).delete(eq(testInfo), captor.capture());
            assertEquals(mockTestResponse, captor.getValue());
        }

        @Test
        void 탈퇴_실패_UserDtoNull() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패_UserInfoNull() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }
}
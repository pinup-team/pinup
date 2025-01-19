package kr.co.pinup.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.PinupApplication;
import kr.co.pinup.users.controller.UserController;
import kr.co.pinup.users.error.UnauthorizedException;
import kr.co.pinup.users.error.UserNotFoundException;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.enums.UserRole;
import kr.co.pinup.users.oauth.OAuthProvider;
import kr.co.pinup.users.service.UserService;
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
public class UserTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Autowired
    private static JdbcTemplate jdbcTemplate;

    private MockHttpSession session;
    private ObjectMapper objectMapper;

    private UserInfo testInfo = new UserInfo("네이버TestUser", OAuthProvider.NAVER, UserRole.ROLE_USER);
    private UserDto testDto = new UserDto("test", "test@naver.com", "네이버TestUser", OAuthProvider.NAVER, UserRole.ROLE_USER);

    private UserInfo mockTestInfo = new UserInfo("mockNickname", OAuthProvider.NAVER, UserRole.ROLE_USER);
    private UserDto mockTestDto = new UserDto("mock", "mock@naver.com", "mockNickname", OAuthProvider.NAVER, UserRole.ROLE_USER);

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("INSERT INTO users (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
                "test", "test@naver.com", "네이버TestUser", "NAVER", "123456789","ROLE_USER");

        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        session = new MockHttpSession();
        objectMapper = new ObjectMapper();
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("DELETE FROM users");
        session.clearAttributes();
    }

    @Test
    void testLogin() {
        // Given
        String expectedView = "users/login";

        // When
        String view = userController.login();

        // Then
        assertEquals(expectedView, view);
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class findUser {
        @Test
        void 조회_성공() throws Exception { // CHECK
            given(userService.findUser(testInfo)).willReturn(testDto);

            MockHttpSession session = new MockHttpSession();

            mockMvc.perform(get("/users/profile")
                            .session(session)
                            .sessionAttr("userInfo", testInfo))
                    .andExpect(status().isOk())
//                    .andExpect(view().name("error"))
                    .andExpect(view().name("users/profile"))
                    .andExpect(model().attribute("profile", testDto))
                    .andDo(print());
        }

        @Test
        void 조회_사용자_없음() throws Exception {
            UserInfo userInfo = UserInfo.builder()
                    .nickname("고독한치타")
                    .provider(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            given(userService.findUser(userInfo)).willReturn(null);

            MockHttpSession session = new MockHttpSession();
            session.setAttribute("userInfo", userInfo);

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
            given(userService.findUser(mockTestInfo)).willReturn(null);

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
            given(userService.update(testInfo, testDto)).willReturn(testDto);

            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(new ObjectMapper().writeValueAsString(testDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("닉네임이 변경되었습니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_중복닉네임() throws Exception {
            UserDto userDto = new UserDto("test", "test@naver.com", "조용한고래", OAuthProvider.NAVER, UserRole.ROLE_USER);

            given(userService.update(testInfo, userDto)).willThrow(new IllegalArgumentException("\""+userDto.getNickname()+"\"은 중복된 닉네임입니다."));
//            given(userService.update(mockUserInfo, userDto)).willReturn(userDto);

            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(userDto)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("중복된 닉네임입니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_UserInfo없음() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", eq(null))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testDto)))
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
            given(userService.delete(testInfo, testDto)).willReturn(true);

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("탈퇴 성공"))
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패_UserNotFound() throws Exception {
            UserDto mockTestDto = new UserDto("test", "test@naver.com", "testNickname", OAuthProvider.NAVER, UserRole.ROLE_USER); // 실제 UserDto 객체 생성

            given(userService.delete(mockTestInfo, testDto)).willThrow(new UserNotFoundException("사용자를 찾을 수 없습니다."));

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", mockTestInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isNotFound())
                    .andExpect(content().string("사용자를 찾을 수 없습니다."))
                    .andDo(print());

            ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
            verify(userService).delete(eq(testInfo), captor.capture());
            assertEquals(mockTestDto, captor.getValue());
        }

        @Test
        void 탈퇴_실패_EmailNotEqual() throws Exception {
            given(userService.delete(testInfo, mockTestDto)).willThrow(new UnauthorizedException("권한이 없습니다."));

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(mockTestDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("권한이 없습니다."))
                    .andDo(print());

            ArgumentCaptor<UserDto> captor = ArgumentCaptor.forClass(UserDto.class);
            verify(userService).delete(eq(testInfo), captor.capture());
            assertEquals(mockTestDto, captor.getValue());
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
                            .content(objectMapper.writeValueAsString(testDto)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }
}
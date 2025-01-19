package kr.co.pinup.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import kr.co.pinup.PinupApplication;
import kr.co.pinup.users.controller.UserController;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.enums.UserRole;
import kr.co.pinup.users.oauth.OAuthProvider;
import kr.co.pinup.users.service.UserService;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ContextConfiguration(classes = PinupApplication.class)
public class UserTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockHttpSession session;
    private UserInfo mockUserInfo;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        session = new MockHttpSession();
        mockUserInfo = new UserInfo("testNickname", OAuthProvider.NAVER, UserRole.ROLE_USER);
    }

    @Test
    void testLogin() {
        // Given
        String expectedView = "/users/login";

        // When
        String view = userController.login();

        // Then
        assertEquals(expectedView, view);
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class findUser {
        @Test
        void 조회_성공() throws Exception {
            UserInfo userInfo = UserInfo.builder()
                    .nickname("어리석은대추나무")
                    .provider(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            UserDto userDto = UserDto.builder()
                    .name("김도희")
                    .email("runa0601019@naver.com")
                    .nickname("어리석은대추나무")
                    .providerType(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            given(userService.findUser(userInfo)).willReturn(userDto);

            mockMvc.perform(get("/users/profile")
                            .sessionAttr("userInfo", userInfo))
                    .andExpect(status().isOk())
                    .andExpect(view().name("/users/profile"))
                    .andExpect(model().attribute("profile", userDto))
                    .andDo(print());
        }

        @Test
        void 조회_UserInfo없음() throws Exception {
            mockMvc.perform(get("/users/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "로그인 정보가 없습니다."))
                    .andDo(print());
        }

        @Test
        void 조회_UserInfo정상_유저정보없음() throws Exception {
            given(userService.findUser(mockUserInfo)).willReturn(null);

            mockMvc.perform(get("/users/profile")
                            .sessionAttr("userInfo", mockUserInfo))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "사용자를 찾을 수 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("유저 수정 테스트")
    class updateUser {
        private UserInfo mockUserInfo = UserInfo.builder()
                .nickname("testNickname")
                .provider(OAuthProvider.NAVER)
                .role(UserRole.ROLE_USER)
                .build();

        @Test
        void 수정_성공() throws Exception {
            UserInfo userInfo = UserInfo.builder()
                    .nickname("어리석은대추나무")
                    .provider(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            UserDto userDto = UserDto.builder()
                    .name("김도희")
                    .email("runa0601019@naver.com")
                    .nickname("완벽한나무늘보")
                    .providerType(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            ObjectMapper objectMapper = new ObjectMapper();

            String userDtoJson = objectMapper.writeValueAsString(userDto);

            given(userService.update(mockUserInfo, userDto))
                    .willReturn(UserDto.builder()
                            .nickname("newNickname")
                            .build());

            mockMvc.perform(MockMvcRequestBuilders.patch("/users")
                            .sessionAttr("userInfo", userInfo)
                            .contentType("application/json")
                            .content(userDtoJson))
                    .andExpect(status().isOk())
                    .andExpect(content().string("닉네임이 변경되었습니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_중복닉네임() throws Exception {

        }

        @Test
        void 수정_실패_UserInfo없음() throws Exception {
            UserInfo userInfo = null;
        }
    }

    @Nested
    @DisplayName("유저 탈퇴 테스트")
    class deleteUser {
        private UserInfo mockUserInfo = UserInfo.builder()
                .nickname("testNickname")
                .provider(OAuthProvider.NAVER)
                .role(UserRole.ROLE_USER)
                .build();

        @Test
        void 탈퇴_성공() throws Exception {
            UserInfo userInfo = UserInfo.builder()
                    .nickname("어색한장미꽃")
                    .provider(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            UserDto userDto = UserDto.builder()
                    .name("김도희")
                    .email("runa0601019@naver.com")
                    .nickname("완벽한나무늘보")
                    .providerType(OAuthProvider.NAVER)
                    .role(UserRole.ROLE_USER)
                    .build();

            given(userService.update(mockUserInfo, userDto))
                    .willReturn(UserDto.builder()
                            .nickname("newNickname")
                            .build());

            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .sessionAttr("userInfo", userInfo)
                            .contentType("application/json")
                            .content("{\"nickname\":\"newNickname\"}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("닉네임이 변경되었습니다."))
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패() throws Exception {

        }

        @Test
        void 탈퇴_실패_UserInfo없음() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/users")
                            .contentType("application/json"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }
}
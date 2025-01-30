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

    private MemberInfo testInfo = MemberInfo.builder()
            .nickname("네이버TestMember")
            .provider(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    private MemberRequest testRequest = MemberRequest.builder()
            .name("test")
            .email("test@naver.com")
            .nickname("네이버TestMember")
            .providerType(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    private MemberResponse testResponse = MemberResponse.builder()
            .id(1L)
            .name("test")
            .email("test@naver.com")
            .nickname("네이버TestMember")
            .providerType(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    private MemberInfo mockTestInfo = MemberInfo.builder()
            .nickname("mockNickname")
            .provider(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    private MemberRequest mockTestRequest = MemberRequest.builder()
            .name("mock")
            .email("mock@naver.com")
            .nickname("mockNickname")
            .providerType(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    private MemberResponse mockTestResponse = MemberResponse.builder()
            .id(1L)
            .name("mock")
            .email("mock@naver.com")
            .nickname("mockNickname")
            .providerType(OAuthProvider.NAVER)
            .role(MemberRole.ROLE_USER)
            .build();

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.update("INSERT INTO members (name, email, nickname, provider_type, provider_id, role) VALUES (?, ?, ?, ?, ?, ?)",
                "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, "123456789", MemberRole.ROLE_USER);

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
        String expectedView = "/views/members/login";

        // When
        String view = memberController.login();

        // Then
        assertEquals(expectedView, view);
    }

    @Nested
    @DisplayName("유저 조회 테스트")
    class findMember {
        @Test
        void 조회_성공() throws Exception { // CHECK
            given(memberService.findMember(testInfo)).willReturn(testResponse);

            MockHttpSession session = new MockHttpSession();

            mockMvc.perform(get("/members/profile")
                            .session(session)
                            .sessionAttr("memberInfo", testInfo))
                    .andExpect(status().isOk())
//                    .andExpect(view().name("error"))
                    .andExpect(view().name("/view/members/profile"))
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

            given(memberService.findMember(memberInfo)).willReturn(null);

            MockHttpSession session = new MockHttpSession();
            session.setAttribute("memberInfo", memberInfo);

            mockMvc.perform(get("/members/profile")
                            .session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "사용자를 찾을 수 없습니다."))
                    .andDo(print());
        }

        @Test
        void 조회_MemberInfo없음() throws Exception { // CHECK
            mockMvc.perform(get("/members/profile"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "로그인 정보가 없습니다."))
                    .andDo(print());
        }

        @Test
        void 조회_MemberInfo있음_유저정보없음() throws Exception {
            given(memberService.findMember(mockTestInfo)).willReturn(null);

            mockMvc.perform(get("/members/profile")
                            .sessionAttr("memberInfo", mockTestInfo))
                    .andExpect(status().isOk())
                    .andExpect(view().name("error"))
                    .andExpect(model().attribute("message", "사용자를 찾을 수 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("유저 수정 테스트") // CHECK
    class updateMember {
        @Test
        void 수정_성공() throws Exception {
            given(memberService.update(testInfo, testRequest)).willReturn(testResponse);

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/members")
                            .sessionAttr("memberInfo", testInfo)
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

            mockMvc.perform(MockMvcRequestBuilders.patch("/api/members")
                            .sessionAttr("memberInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(memberRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("중복된 닉네임입니다."))
                    .andDo(print());
        }

        @Test
        void 수정_실패_MemberInfo없음() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.patch("/api/members")
                            .sessionAttr("memberInfo", eq(null))
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }

    @Nested
    @DisplayName("유저 탈퇴 테스트") // CHECK
    class deleteMember {
        @Test
        void 탈퇴_성공() throws Exception {
            given(memberService.delete(testInfo, testRequest)).willReturn(true);

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/members")
                            .sessionAttr("memberInfo", testInfo)
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isOk())
                    .andExpect(content().string("탈퇴 성공"))
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패_MemberNotFound() throws Exception {
            given(memberService.delete(mockTestInfo, mockTestRequest)).willThrow(new MemberNotFoundException());

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/members")
                            .sessionAttr("memberInfo", mockTestInfo)
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

            mockMvc.perform(MockMvcRequestBuilders.delete("/api/members")
                            .sessionAttr("memberInfo", testInfo)
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
        void 탈퇴_실패_MemberDtoNull() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/members")
                            .sessionAttr("memberInfo", testInfo)
                            .contentType("application/json")
                            .content(""))
                    .andExpect(status().isBadRequest())
                    .andDo(print());
        }

        @Test
        void 탈퇴_실패_MemberInfoNull() throws Exception {
            mockMvc.perform(MockMvcRequestBuilders.delete("/api/members")
                            .contentType("application/json")
                            .content(objectMapper.writeValueAsString(testRequest)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string("로그인 정보가 없습니다."))
                    .andDo(print());
        }
    }
}
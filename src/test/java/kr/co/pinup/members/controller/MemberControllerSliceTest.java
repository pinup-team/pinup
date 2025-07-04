package kr.co.pinup.members.controller;

import kr.co.pinup.config.LoggerConfig;
import kr.co.pinup.config.SecurityConfigTest;
import kr.co.pinup.members.custom.WithMockMember;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import({SecurityConfigTest.class, LoggerConfig.class})
@WebMvcTest(MemberController.class)
public class MemberControllerSliceTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Login 페이지 이동")
    void login() throws Exception {
        String viewName = mockMvc.perform(get("/members/login")).andReturn().getModelAndView().getViewName();

        assertEquals("views/members/login", viewName);
    }

    @Test
    @WithMockMember(nickname = "test", provider = OAuthProvider.NAVER, role = MemberRole.ROLE_USER)
    @DisplayName("마이 페이지 이동")
    void memberProfile() throws Exception {
        MemberResponse testResponse = new MemberResponse(1L, "test", "test@naver.com", "네이버TestMember", OAuthProvider.NAVER, MemberRole.ROLE_USER, false);

        when(memberService.findMember(any(MemberInfo.class))).thenReturn(testResponse);

        mockMvc.perform(get("/members/profile"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(view().name("views/members/profile"))
                .andExpect(model().attributeExists("profile"))
                .andExpect(model().attribute("profile", is(testResponse)));
    }
}
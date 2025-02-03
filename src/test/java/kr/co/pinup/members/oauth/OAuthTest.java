package kr.co.pinup.members.oauth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.PinupApplication;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.members.controller.MemberController;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.enums.MemberRole;
import kr.co.pinup.members.service.MemberService;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.google.GoogleLoginParams;
import kr.co.pinup.oauth.naver.NaverLoginParams;
import org.junit.jupiter.api.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ContextConfiguration(classes = PinupApplication.class)
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
public class OAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private MemberService memberService;

    @InjectMocks
    private MemberController memberController;

    private static WireMockServer wireMockServer;

//    @RegisterExtension
//    static WireMockExtension wireMockServer = WireMockExtension.newInstance().options(wireMockConfig().port(8888)).build();


    private OauthConfig oauthConfig;
    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;
    private OauthConfig.Registration googleRegistration;
    private OauthConfig.Provider googleProvider;
    HttpSession session;

    private ObjectMapper objectMapper;

    public OAuthTest(MockMvc mockMvc, MemberService memberService, OauthConfig oauthConfig) {
        this.mockMvc = mockMvc;
        this.memberService = memberService;
        this.oauthConfig = oauthConfig;

        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");
        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.googleProvider = oauthConfig.getProvider().get("google");
    }

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(options().port(8888));  // WireMockServer 객체 생성
        wireMockServer.start();
        System.out.println("WireMock server started on port 8888");

        // Naver Token 요청 모킹
        wireMockServer.stubFor(post(urlEqualTo("/test/members/naver/token"))
//                wireMockServer.stubFor(post(urlEqualTo(naverProvider.getTokenUri()))
                .withQueryParam("client_id", matching(naverRegistration.getClientId()))
                .withQueryParam("client_secret", matching(naverRegistration.getClientSecret()))
                .withQueryParam("grant_type", matching(naverRegistration.getAuthorizationGrantType()))
                .withQueryParam("code", matching("oauthTestCode"))
                .withQueryParam("state", matching("oauthTestState"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"access_token\": \"mock-access-token-oauthTestCode\", \"refresh_token\": \"mock-refresh-token\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\" }")));

        // Naver MemberInfo 요청 모킹
        wireMockServer.stubFor(post(urlEqualTo("/test/members/naver/userInfo"))
//                wireMockServer.stubFor(post(urlEqualTo(naverProvider.getUserInfoUri()))
                .withHeader("Authorization", matching("Bearer mock-access-token-oauthTestCode"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"response\": { \"id\": \"123456789\", \"name\": \"test user\", \"email\": \"testuser@naver.com\" }}")));

        // Google Token 요청 모킹
        wireMockServer.stubFor(post(urlEqualTo("/test/members/google/token"))
//                wireMockServer.stubFor(post(urlEqualTo(googleProvider.getTokenUri()))
                .withQueryParam("client_id", matching(googleRegistration.getClientId()))
                .withQueryParam("client_secret", matching(googleRegistration.getClientSecret()))
                .withQueryParam("grant_type", matching(googleRegistration.getAuthorizationGrantType()))
                .withQueryParam("code", matching("oauthTestCode"))
                .withQueryParam("redirect_uri", matching(googleRegistration.getRedirectUri()))
                .willReturn(aResponse().withStatus(200).withBody("{ \"access_token\": \"mock-access-token-oauthTestCode\", \"refresh_token\": \"mock-refresh-token\", \"token_type\": \"Bearer\", \"expires_in\": \"3600\", \"scope\": \"read\" }")));

        // Google MemberInfo 요청 모킹
        wireMockServer.stubFor(post(urlEqualTo("/test/members/google/userInfo"))
//                wireMockServer.stubFor(post(urlEqualTo(googleProvider.getUserInfoUri()))
                .withHeader("Authorization", matching("Bearer mock-access-token-oauthTestCode"))
                .willReturn(aResponse().withStatus(200).withBody("{ \"id\": \"a1b2c3d4e5_f6g7h8\", \"name\": \"test user\", \"email\": \"testuser@google.com\" }")));

        objectMapper = new ObjectMapper();}

    @AfterEach
    void tearDown() {
        wireMockServer.stop();  // 테스트 후 WireMock 서버 정지
    }

    @Nested
    @DisplayName("OAuth 로그인/회원가입 테스트")
    class OAuthLoginTests {

        private String code = "oauthTestCode";
        private String state = "oauthTestState";

        @Test
        void naverLogin() throws Exception {
            NaverLoginParams naverParams = new NaverLoginParams();
            naverParams.setCode(code);
            naverParams.setState(state);

            MemberInfo memberInfo = MemberInfo.builder()
                    .nickname(anyString())
                    .provider(OAuthProvider.NAVER)
                    .role(MemberRole.ROLE_USER)
                    .build();
            given(memberService.login(naverParams, eq(null))).willReturn(memberInfo);

            System.out.println("naverTokenUri: " + naverProvider.getTokenUri());
            mockMvc.perform(get("/api/members/oauth/naver")
                            .content(objectMapper.writeValueAsString(naverParams))
                    )
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/"))
                    .andDo(print());
        }

        @Test
        void googleLogin() throws Exception {
            GoogleLoginParams googleParams = new GoogleLoginParams();
            googleParams.setCode(code);
            googleParams.setState(state);

            MemberInfo memberInfo = MemberInfo.builder()
                    .nickname(anyString())
                    .provider(OAuthProvider.GOOGLE)
                    .role(MemberRole.ROLE_USER)
                    .build();
            given(memberService.login(googleParams, eq(null))).willReturn(memberInfo);

            mockMvc.perform(get("/api/members/oauth/google")
                            .content(objectMapper.writeValueAsString(googleParams))
                    )
                    .andExpect(status().is3xxRedirection()) // 리디렉션 확인
                    .andExpect(redirectedUrl("/")) // "/"로 리디렉션
                    .andDo(print());
        }
    }
}

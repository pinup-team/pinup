package kr.co.pinup.users.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.users.model.*;
import kr.co.pinup.users.model.enums.Provider;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OauthConfig oauthConfig;

    private OauthConfig.Registration googleRegistration;

    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;

    @PostConstruct // Java의 표준 애노테이션으로, 클래스가 초기화된 직후 실행되어야 하는 메서드를 지정할 때 사용
    private void initOauthConfig() {
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");

        this.googleRegistration = oauthConfig.getRegistration().get("google");
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    @Override
    public String makeNaverAuthUri(HttpSession session) {
        log.info("Make AuthUri: naver");
        String state = generateState();
        session.setAttribute("state", state);

        // Authorization URI 생성
        return String.format(
//                "%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
//                URLEncoder.encode(naverProvider.getAuthorizationUri(), StandardCharsets.UTF_8),
                "https://nid.naver.com/oauth2.0/authorize?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                URLEncoder.encode(naverRegistration.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(naverRegistration.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );
    }

    @Override
    public String makeGoogleAuthUri() {
        log.info("Make AuthUri: google");

        // Authorization URI 생성
        return String.format(
                "https://accounts.google.com/o/oauth2/auth?client_id=%s"
                        + "&redirect_uri=%s&response_type=code"
                        + "&scope=https://www.googleapis.com/auth/userinfo.email+profile",
                URLEncoder.encode(googleRegistration.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(googleRegistration.getRedirectUri(), StandardCharsets.UTF_8)
        );
    }

    @Override
    public UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session) {
        log.info("Oauth: naver");

        if (!isValidState(session, state)) {
            return null;
        }

        String accessToken = getAccessToken(code);
        OauthUserDto oauthUserDto = getUserInfo(accessToken);

        UserInfo userInfo = new UserInfo();
        if (userRepository.existsByProviderId(oauthUserDto.getResponse().getId())) {
            UserEntity user = userRepository.findByProviderId(oauthUserDto.getResponse().getId());
            userInfo = UserInfo.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .build();
        } else {
            userInfo = save(oauthUserDto, Provider.NAVER);
        }
        return userInfo;
    }

    @Override
    public UserInfo save(OauthUserDto oauthUserDto, Provider provider) {
        OauthUserDto.Response response = oauthUserDto.getResponse();

        UserEntity user = UserEntity.builder()
                .name(response.getName())
                .email(response.getEmail())
                .gender(response.getGender())
                .age(response.getAge())
                .providerType(provider)
                .providerId(response.getId())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserInfo.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    private boolean isValidState(HttpSession session, String state) {
        String sessionState = (String) session.getAttribute("state");
        if (sessionState.equals(state)) {
            return true;
        } else {
            return false;
        }
    }

    private String getAccessToken(String code) {
        String accessTokenUrl =
//                naverProvider.getTokenUri()
                "https://nid.naver.com/oauth2.0/token"
                        + "?grant_type=authorization_code"
                        + "&client_id=" + naverRegistration.getClientId()
                        + "&client_secret=" + naverRegistration.getClientSecret()
                        + "&state=" + generateState()
                        + "&code=" + code;

        WebClient webClient = WebClient.builder().build();
        TokenReponse tokenReponse = webClient.post()
                .uri(accessTokenUrl)
                .retrieve()
                .bodyToMono(TokenReponse.class)
                .block();

        return tokenReponse.getAccess_token();
    }

    private OauthUserDto getUserInfo(String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me";

        WebClient webClient = WebClient.builder().build();
        OauthUserDto userDto = webClient.get()
                .uri(url)
//                .uri(naverProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(OauthUserDto.class).block();
        System.out.println("userDto" + userDto);
        return userDto;
    }
}

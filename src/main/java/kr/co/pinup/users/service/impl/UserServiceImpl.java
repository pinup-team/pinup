package kr.co.pinup.users.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.users.model.*;
import kr.co.pinup.users.model.enums.OAuthProvider;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
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

    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;

    private OauthConfig.Registration googleRegistration;
    private OauthConfig.Provider googleProvider;

    @PostConstruct // Java의 표준 애노테이션으로, 클래스가 초기화된 직후 실행되어야 하는 메서드를 지정할 때 사용
    private void initOauthConfig() {
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");

        this.googleRegistration = oauthConfig.getRegistration().get("google");
        this.googleProvider = oauthConfig.getProvider().get("google");
    }

    private String generateState() {
        SecureRandom random = new SecureRandom();
        return new BigInteger(130, random).toString(32);
    }

    // TODO CustomDialect 성공하면 uri 전달해서 앞에서 바로 사용 가능할 듯
    @Override
    public String makeNaverAuthUri(HttpSession session) {
        log.info("Make AuthUri: naver");
        String state = generateState();
        session.setAttribute("stateNaver", state);

        return String.format(
                "%s?response_type=code&client_id=%s&redirect_uri=%s&state=%s",
                URLEncoder.encode(naverProvider.getAuthorizationUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(naverRegistration.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(naverRegistration.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );
    }

    @Override
    public String makeGoogleAuthUri(HttpSession session) {
        log.info("Make AuthUri: google");
        String state = generateState();
        session.setAttribute("stateGoogle", state);

        // Authorization URI 생성
        return String.format(
                "%s?client_id=%s"
                        + "&redirect_uri=%s&response_type=code"
                        + "&scope=https://www.googleapis.com/auth/userinfo.email+profile+https://www.googleapis.com/auth/user.gender.read+https://www.googleapis.com/auth/user.birthday.read"
                        + "&state=%s",
                googleProvider.getAuthorizationUri(),
                googleRegistration.getClientId(),
                googleRegistration.getRedirectUri(),
                state);
//        return String.format(
//                "%s?client_id=%s"
//                        + "&redirect_uri=%s&response_type=code"
//                        + "&scope=https://www.googleapis.com/auth/userinfo.email+profile+https://www.googleapis.com/auth/user.gender.read+https://www.googleapis.com/auth/user.birthday.read"
//                        + "&state=%s",
//                URLEncoder.encode(googleProvider.getAuthorizationUri(), StandardCharsets.UTF_8),
//                URLEncoder.encode(googleRegistration.getClientId(), StandardCharsets.UTF_8),
//                URLEncoder.encode(googleRegistration.getRedirectUri(), StandardCharsets.UTF_8),
//                URLEncoder.encode(state, StandardCharsets.UTF_8)
//        );
    }

    @Override
    public UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session) {
        log.info("Oauth: naver");

        String accessToken = getAccessToken(code);
        session.setAttribute("accessToken", accessToken);
        NaverDto naverDto = getUserInfo(accessToken);

        UserInfo userInfo = new UserInfo();
        if (userRepository.existsByProviderId(naverDto.getResponse().getId())) {
            UserEntity user = userRepository.findByProviderId(naverDto.getResponse().getId());
            userInfo = UserInfo.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .provider(user.getProviderType())
                    .build();
        } else {
            userInfo = saveUserByNaver(naverDto, OAuthProvider.NAVER);
        }
        return userInfo;
    }

    private String getAccessToken(String code) {
        String accessTokenUrl =
                naverProvider.getTokenUri()
                        + "?grant_type=" + naverRegistration.getAuthorizationGrantType()
                        + "&client_id=" + naverRegistration.getClientId()
                        + "&client_secret=" + naverRegistration.getClientSecret()
                        + "&state=" + generateState()
                        + "&code=" + code;

        WebClient webClient = WebClient.builder().build();
        TokenReponse tokenResponse = webClient.post()
                .uri(accessTokenUrl)
                .retrieve()
                .bodyToMono(TokenReponse.class)
                .block();

        return tokenResponse.getAccess_token();
    }

    private NaverDto getUserInfo(String accessToken) {
        WebClient webClient = WebClient.builder().build();
        NaverDto userDto = webClient.get()
                .uri(naverProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(NaverDto.class).block();
        System.out.println("userDto" + userDto);
        return userDto;
    }

    private UserInfo saveUserByNaver(NaverDto naverDto, OAuthProvider OAuthProvider) {
        NaverDto.Response response = naverDto.getResponse();

        UserEntity user = UserEntity.builder()
                .name(response.getName())
                .email(response.getEmail())
                .providerType(OAuthProvider.NAVER)
                .providerId(response.getId())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserInfo.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .provider(savedUser.getProviderType())
                .build();
    }

    @Override
    public UserInfo oauthGoogle(String code, String state, String error, HttpSession session) {
        log.info("Oauth: google");

        String accessToken = getGoogleAccessToken(code);
        session.setAttribute("accessToken", accessToken);
        GoogleDto googleDto = getGoogleUserInfo(accessToken);

        UserInfo userInfo = new UserInfo();
        if (userRepository.existsByProviderId(googleDto.getSub())) {
            UserEntity user = userRepository.findByProviderId(googleDto.getSub());
            userInfo = UserInfo.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .provider(user.getProviderType())
                    .build();
        } else {
            userInfo = saveUserByGoogle(googleDto, OAuthProvider.GOOGLE);
        }
        return userInfo;
    }

    private String getGoogleAccessToken(String code) {
        String accessTokenUrl =
                googleProvider.getTokenUri()
                        + "?grant_type=" + googleRegistration.getAuthorizationGrantType()
                        + "&client_id=" + googleRegistration.getClientId()
                        + "&client_secret=" + googleRegistration.getClientSecret()
                        + "&code=" + code;

        WebClient webClient = WebClient.builder()
                .defaultHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .build();
        TokenReponse tokenResponse = webClient.post()
                .uri(accessTokenUrl)
                // BodyInserters.fromFormData()를 사용하여 x-www-form-urlencoded 형식으로 데이터를 전송, 각 파라미터를 with() 메서드를 사용하여 폼 데이터로 변환해 추가
                .body(BodyInserters.fromFormData("grant_type", googleRegistration.getAuthorizationGrantType())
                        .with("client_id", googleRegistration.getClientId())
                        .with("client_secret", googleRegistration.getClientSecret())
                        .with("code", code)
                        .with("redirect_uri", googleRegistration.getRedirectUri()))
                .retrieve()
                .bodyToMono(TokenReponse.class)
                .block();

        return tokenResponse.getAccess_token();
    }

    private GoogleDto getGoogleUserInfo(String accessToken) {
        WebClient webClient = WebClient.builder().build();
        GoogleDto userDto = webClient.get()
                .uri(googleProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(GoogleDto.class).block();
        System.out.println("googleDto" + userDto);
        return userDto;
    }

    private UserInfo saveUserByGoogle(GoogleDto googleDto, OAuthProvider OAuthProvider) {
        UserEntity user = UserEntity.builder()
                .name(googleDto.getName())
                .email(googleDto.getEmail())
                .providerType(OAuthProvider.GOOGLE)
                .providerId(googleDto.getSub())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserInfo.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .provider(savedUser.getProviderType())
                .build();
    }

    @Override
    public String logout(HttpSession session, OAuthProvider OAuthProvider) {
        System.out.println("logout " + OAuthProvider);
        String accessTokenUrl = "";
        String response = "";
        if (OAuthProvider.equals(OAuthProvider.NAVER)) {
            accessTokenUrl =
                    naverProvider.getTokenUri()
                            + "?grant_type=" + "delete"
                            + "&client_id=" + naverRegistration.getClientId()
                            + "&client_secret=" + naverRegistration.getClientSecret()
                            + "&access_token=" + session.getAttribute("accessToken")
                            + "&service_provider=" + OAuthProvider.toString();
            System.out.println("accessTokenUrl" + accessTokenUrl);
            WebClient webClient = WebClient.builder().build();
            TokenReponse tokenReponse = webClient.post()
                    .uri(accessTokenUrl)
                    .retrieve()
                    .bodyToMono(TokenReponse.class)
                    .block();
            response=tokenReponse.getAccess_token();
        } else if (OAuthProvider.equals(OAuthProvider.GOOGLE)) {
            // TODO google logout api 찾기, 안되면 session 날리기
            accessTokenUrl = "https://www.googleapis.com/revoke" + "?token=";
//            accessTokenUrl = googleProvider.getAuthorizationUri() + "?prompt=consent";
            System.out.println("accessTokenUrl" + accessTokenUrl);
            WebClient webClient = WebClient.builder().build();
            response = webClient.post()
                    .uri(accessTokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue("token=" + session.getAttribute("accessToken"))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("Google revoke response: " + response);
        }

        session.invalidate();
        return response;
    }
}

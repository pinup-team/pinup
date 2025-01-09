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
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigInteger;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OauthConfig oauthConfig;

    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;

    private OauthConfig.Registration googleRegistration;

    private String sessionStateNaver = "";
    private String sessionStateGoogle = "";

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
        session.setAttribute("stateNaver", state);
        sessionStateNaver = state;

        // 상태값 저장 시 로그 추가
        log.info("Generated state: {}", state);

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
    public String makeGoogleAuthUri(HttpSession session) {
        log.info("Make AuthUri: google");
        String state = generateState();
        session.setAttribute("stateGoogle", state);
        sessionStateGoogle = state;

        // 상태값 저장 시 로그 추가
        log.info("Generated state: {}", state);

        // Authorization URI 생성
        return String.format(
                "https://accounts.google.com/o/oauth2/auth?client_id=%s"
                        + "&redirect_uri=%s&response_type=code"
//                        + "&scope=https://www.googleapis.com/auth/userinfo.email+profile"
                        + "&scope=https://www.googleapis.com/auth/userinfo.email+profile+https://www.googleapis.com/auth/user.gender.read+https://www.googleapis.com/auth/user.birthday.read"
                        + "&state=%s",
                URLEncoder.encode(googleRegistration.getClientId(), StandardCharsets.UTF_8),
                URLEncoder.encode(googleRegistration.getRedirectUri(), StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8)
        );
    }

    @Override
    public UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session) {
        log.info("Oauth: naver");

        if (!isValidStateNaver(session, state)) {
            System.out.println("isValidState false");
            return null;
        }

        System.out.println("getAccessToken");
        String accessToken = getAccessToken(code);
        NaverDto naverDto = getUserInfo(accessToken);

        UserInfo userInfo = new UserInfo();
        if (userRepository.existsByProviderId(naverDto.getResponse().getId())) {
            UserEntity user = userRepository.findByProviderId(naverDto.getResponse().getId());
            userInfo = UserInfo.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .build();
        } else {
            userInfo = saveUserByNaver(naverDto, Provider.NAVER);
        }
        return userInfo;
    }

    @Override
    public UserInfo oauthGoogle(String code, String state, String error, HttpSession session) {
        log.info("Oauth: google");

        if (!isValidStateGoogle(session, state)) {
            return null;
        }

        String accessToken = getGoogleAccessToken(code);
        GoogleDto googleDto = getGoogleUserInfo(accessToken);

        UserInfo userInfo = new UserInfo();
        if (userRepository.existsByProviderId(googleDto.getSub())) {
            UserEntity user = userRepository.findByProviderId(googleDto.getSub());
            userInfo = UserInfo.builder()
                    .email(user.getEmail())
                    .name(user.getName())
                    .role(user.getRole())
                    .build();
        } else {
            userInfo = saveUserByGoogle(googleDto, Provider.GOOGLE);
        }
        return userInfo;
    }

    @Override
    public UserInfo saveUserByNaver(NaverDto naverDto, Provider provider) {
        NaverDto.Response response = naverDto.getResponse();

        UserEntity user = UserEntity.builder()
                .name(response.getName())
                .email(response.getEmail())
                .gender(response.getGender())
                .birthyear(response.getBirthyear())
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

    private boolean isValidStateNaver(HttpSession session, String state) {
        String sessionState = (String) session.getAttribute("stateNaver");

        // sessionState가 null이거나 비어있는 경우에 대비
        if (sessionState == null || sessionState.isEmpty()) {
            log.error("Session state is null or empty");
            return false;
        }

        log.info("State from session: {}", sessionState);
        log.info("State from code: {}", this.sessionStateNaver);
        log.info("State from request: {}", state);
        if (sessionState.equals(state)) {
            this.sessionStateNaver = "";
            return true;
        } else {
            log.error("State mismatch: expected {}, but got {}", sessionState, state);
            return false;
        }
    }
    private boolean isValidStateGoogle(HttpSession session, String state) {
        String sessionState = (String) session.getAttribute("stateGoogle");

        // sessionState가 null이거나 비어있는 경우에 대비
        if (sessionState == null || sessionState.isEmpty()) {
            log.error("Session state is null or empty");
            return false;
        }

        log.info("State from session: {}", sessionState);
        log.info("State from code: {}", this.sessionStateGoogle);
        log.info("State from request: {}", state);
        if (sessionState.equals(state)) {
            this.sessionStateGoogle = "";
            return true;
        } else {
            log.error("State mismatch: expected {}, but got {}", sessionState, state);
            return false;
        }
    }

    private String getAccessToken(String code) {
        System.out.println("getAccessToken");
        String accessTokenUrl =
//                naverProvider.getTokenUri()
                "https://nid.naver.com/oauth2.0/token"
                        + "?grant_type=" + naverRegistration.getAuthorizationGrantType()
                        + "&client_id=" + naverRegistration.getClientId()
                        + "&client_secret=" + naverRegistration.getClientSecret()
                        + "&state=" + generateState()
                        + "&code=" + code;

        log.warn("accessTokenUrl", accessTokenUrl);
        System.out.println("accessTokenUrl : " + accessTokenUrl);
        WebClient webClient = WebClient.builder().build();
        TokenReponse tokenReponse = webClient.post()
                .uri(accessTokenUrl)
                .retrieve()
                .bodyToMono(TokenReponse.class)
                .block();

        return tokenReponse.getAccess_token();
    }

    private NaverDto getUserInfo(String accessToken) {
        String url = "https://openapi.naver.com/v1/nid/me";

        WebClient webClient = WebClient.builder().build();
        NaverDto userDto = webClient.get()
                .uri(url)
//                .uri(naverProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(NaverDto.class).block();
        System.out.println("userDto" + userDto);
        return userDto;
    }

    @Override
    public UserInfo saveUserByGoogle(GoogleDto googleDto, Provider provider) {
        UserEntity user = UserEntity.builder()
                .name(googleDto.getName())
                .email(googleDto.getEmail())
                .gender(googleDto.getGender())
                .birthyear(googleDto.getBirthday())
                .providerType(provider)
                .providerId(googleDto.getSub())
                .build();

        UserEntity savedUser = userRepository.save(user);
        return UserInfo.builder()
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .build();
    }

    private String getGoogleAccessToken(String code) {
        String accessTokenUrl =
//                naverProvider.getTokenUri()
                "https://oauth2.googleapis.com/token"
                        + "?grant_type=" + googleRegistration.getAuthorizationGrantType()
                        + "&client_id=" + googleRegistration.getClientId()
                        + "&client_secret=" + googleRegistration.getClientSecret()
                        + "&code=" + code;

        WebClient webClient = WebClient.builder()
                .defaultHeader("Content-type", "application/x-www-form-urlencoded;charset=utf-8")
                .build();
        TokenReponse tokenReponse = webClient.post()
                .uri(accessTokenUrl)
                // BodyInserters.fromFormData()를 사용하여 x-www-form-urlencoded 형식으로 데이터를 전송, 각 파라미터를 with() 메서드를 사용하여 폼 데이터로 변환해 추가
                .body(BodyInserters.fromFormData("grant_type", googleRegistration.getAuthorizationGrantType())
                        .with("client_id", googleRegistration.getClientId())
                        .with("client_secret", googleRegistration.getClientSecret())
                        .with("code", code)
                        .with("redirect_uri", googleRegistration.getRedirectUri()))
//                .bodyValue(new TokenRequest(googleRegistration.getAuthorizationGrantType(), googleRegistration.getClientId(), googleRegistration.getClientSecret(), code, generateState()))
                .retrieve()
                .bodyToMono(TokenReponse.class)
                .block();

        return tokenReponse.getAccess_token();
    }

    //    java.lang.NullPointerException: Cannot invoke "kr.co.pinup.users.model.NaverDto$Response.getId()" because the return value of "kr.co.pinup.users.model.NaverDto.getResponse()" is null
    // TODO google받아오기 완료 사용자 정보 빼내는 것만 할 것
    private GoogleDto getGoogleUserInfo(String accessToken) {
        String url = "https://www.googleapis.com/oauth2/v3/userinfo";
//        String url = "https://www.googleapis.com/userinfo/v2/me";

        WebClient webClient = WebClient.builder().build();
        GoogleDto userDto = webClient.get()
                .uri(url)
//                .uri(naverProvider.getUserInfoUri())
                .header("Authorization", "Bearer " + accessToken)
                .retrieve().bodyToMono(GoogleDto.class).block();
        GoogleDto googleDto = getGoogleUserInfoWithPeopleApi(accessToken);

        userDto.setGender(googleDto.getGender());
        userDto.setBirthday(googleDto.getBirthday());
        System.out.println("googleDto" + userDto);
        return userDto;
    }

    // todo google에서 birthyear gender만 받아오기
    private GoogleDto getGoogleUserInfoWithPeopleApi(String accessToken) {
//        String url = "https://people.googleapis.com/v1/people/me?personFields=gender,birthday,email,names";
        System.out.println("getGoogleUserInfoWithPeopleApi");
        String url = "https://people.googleapis.com/v1/people/me?personFields=genders,birthdays";

        WebClient webClient = WebClient.builder().build();
        GoogleDto userDto = webClient.get()
                .uri(url)
                .header("Authorization", "Bearer " + accessToken) // 액세스 토큰 포함
                .retrieve()
                .bodyToMono(GoogleDto.class)
                .block(); // 비동기 요청이므로 동기 방식으로 결과를 받음

        System.out.println("userDto: " + userDto);
        return userDto;
    }

    // 나이 계산은 나중에 통계 낼때 할 수 잇는지 보기
    private int calculateAge(String birthday) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate birthDate = LocalDate.parse(birthday, formatter);
        LocalDate currentDate = LocalDate.now();

        Period period = Period.between(birthDate, currentDate);
        return period.getYears();  // 나이 반환
    }
}

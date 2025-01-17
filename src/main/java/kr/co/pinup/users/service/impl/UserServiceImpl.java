package kr.co.pinup.users.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.users.model.UserEntity;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.UserRepository;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OAuthService OAuthService;
    private final OauthConfig oauthConfig;

    private OauthConfig.Registration naverRegistration;
    private OauthConfig.Provider naverProvider;

    @PostConstruct // Java의 표준 애노테이션으로, 클래스가 초기화된 직후 실행되어야 하는 메서드를 지정할 때 사용
    private void initOauthConfig() {
        this.naverRegistration = oauthConfig.getRegistration().get("naver");
        this.naverProvider = oauthConfig.getProvider().get("naver");
    }

    @Override
    public UserInfo login(OAuthLoginParams params, HttpSession session) {
        OAuthResponse oAuthResponse = OAuthService.request(params, session);
        UserEntity user = findOrCreateUser(oAuthResponse);
        return UserInfo.builder()
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .provider(user.getProviderType())
                .build();
    }

    private UserEntity findOrCreateUser(OAuthResponse oAuthResponse) {
        return userRepository.findByEmail(oAuthResponse.getEmail())
                .orElseGet(() -> newUser(oAuthResponse));
    }

    private UserEntity newUser(OAuthResponse oAuthResponse) {
        UserEntity user = UserEntity.builder()
                .name(oAuthResponse.getName())
                .email(oAuthResponse.getEmail())
                .providerType(oAuthResponse.getOAuthProvider())
                .providerId(oAuthResponse.getId())
                .build();

        return userRepository.save(user);
    }

    @Override
    public boolean logout(OAuthProvider oAuthProvider, HttpSession session) {
        System.out.println("logout " + oAuthProvider);
        boolean response = OAuthService.revoke(oAuthProvider, session);

//        String accessToken = (String) session.getAttribute("accessToken");
//        if (accessToken == null || accessToken.isEmpty()) {
//            throw new IllegalArgumentException("Access token is missing or invalid.");
//        }
//
//        String accessTokenUrl;
//        String response;
//
//        switch (oAuthProvider) {
//            case GOOGLE:
//                accessTokenUrl = "https://oauth2.googleapis.com/revoke?token=" + accessToken;
//                response = revokeToken(accessTokenUrl, String.class);
//                break;
//            case NAVER:
//                response = revokeToken(naverLogout(accessToken), TokenReponse.class).getAccess_token();
//                break;
//            default:
//                throw new UnsupportedOperationException("Unsupported OAuth provider: " + oAuthProvider);
//        }

        if (response) {
            session.invalidate();
            return response;
        } else {
            return response;
        }
    }

    private String naverLogout(String accessToken) {
        return naverProvider.getTokenUri()
                + "?grant_type=" + "delete"
                + "&client_id=" + naverRegistration.getClientId()
                + "&client_secret=" + naverRegistration.getClientSecret()
                + "&access_token=" + accessToken
                + "&service_provider=" + OAuthProvider.NAVER;
    }

    private <T> T revokeToken(String url, Class<T> responseType) {
        WebClient webClient = WebClient.builder().build();
        return webClient.post()
                .uri(url)
                .retrieve()
                .bodyToMono(responseType)
                .block();
    }
}

package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.enums.OAuthProvider;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    String makeNaverAuthUri(HttpSession session);

    String makeGoogleAuthUri(HttpSession session);

    UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session);
    UserInfo oauthGoogle(String code, String state, String error, HttpSession session);

    //    UserInfo save(NaverDto naverDto, Provider provider);
    String logout(HttpSession session, OAuthProvider OAuthProvider);
}

package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.enums.Provider;
import kr.co.pinup.users.model.OauthUserDto;

public interface UserService {

    String makeNaverAuthUri(HttpSession session);

    String makeGoogleAuthUri(HttpSession session);

    UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session);
    UserInfo oauthGoogle(String code, String error, HttpSession session);

    UserInfo save(OauthUserDto oauthUserDto, Provider provider);
}

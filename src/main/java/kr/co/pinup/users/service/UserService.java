package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.users.model.UserInfo;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    UserInfo login(OAuthLoginParams params, HttpSession session);

    boolean logout(OAuthProvider oAuthProvider, HttpSession session);
}

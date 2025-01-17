package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserInfo;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

@Service
public interface UserService {

    UserInfo login(OAuthLoginParams params, HttpSession session);

    UserDto findUser(UserInfo userInfo);

    String makeNickname();

    UserDto update(UserInfo userInfo, UserDto userDto);

    boolean delete(UserInfo userInfo, UserDto userDto);

    boolean logout(OAuthProvider oAuthProvider, HttpSession session);
}

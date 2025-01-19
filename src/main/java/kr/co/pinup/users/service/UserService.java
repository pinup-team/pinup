package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.oauth.OAuthLoginParams;
import kr.co.pinup.users.oauth.OAuthProvider;

public interface UserService {

    UserInfo login(OAuthLoginParams params, HttpSession session);

    String makeNickname();

    UserDto findUser(UserInfo userInfo);

    UserDto update(UserInfo userInfo, UserDto userDto);

    boolean delete(UserInfo userInfo, UserDto userDto);

    boolean logout(OAuthProvider oAuthProvider, HttpSession session);
}

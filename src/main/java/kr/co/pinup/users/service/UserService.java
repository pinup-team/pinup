package kr.co.pinup.users.service;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.model.GoogleDto;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.enums.Provider;
import kr.co.pinup.users.model.NaverDto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
public interface UserService {

    String makeNaverAuthUri(HttpSession session);

    String makeGoogleAuthUri(HttpSession session);

    UserInfo oauthNaver(String code, String state, String error, String error_description, HttpSession session);
    UserInfo oauthGoogle(String code, String state, String error, HttpSession session);

    UserInfo saveUserByNaver(NaverDto naverDto, Provider provider);
    UserInfo saveUserByGoogle(GoogleDto googleDto, Provider provider);
//    UserInfo save(NaverDto naverDto, Provider provider);
}

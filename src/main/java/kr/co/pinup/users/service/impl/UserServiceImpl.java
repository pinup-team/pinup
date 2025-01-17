package kr.co.pinup.users.service.impl;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.config.OauthConfig;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthService;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserEntity;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.UserRepository;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OAuthService OAuthService;

    @Override
    public UserInfo login(OAuthLoginParams params, HttpSession session) {
        OAuthResponse oAuthResponse = OAuthService.request(params, session);
        UserEntity user = findOrCreateUser(oAuthResponse);
        return UserInfo.builder()
                .nickname(user.getNickname())
                .role(user.getRole())
                .provider(user.getProviderType())
                .build();
    }

    @Override
    public UserDto findUser(UserInfo userInfo) {
        return userRepository.findByNickname(userInfo.getNickname())
                .map(userEntity -> {
                    return UserDto.builder()
                            .name(userEntity.getName())
                            .email(userEntity.getEmail())
                            .nickname(userInfo.getNickname())
                            .providerType(userEntity.getProviderType())
                            .role(userEntity.getRole())
                            .build();
                })
                .orElse(null);
    }

    private UserEntity findOrCreateUser(OAuthResponse oAuthResponse) {
        return userRepository.findByEmail(oAuthResponse.getEmail())
                .orElseGet(() -> newUser(oAuthResponse));
    }

    private UserEntity newUser(OAuthResponse oAuthResponse) {
        UserEntity user = UserEntity.builder()
                .name(oAuthResponse.getName())
                .email(oAuthResponse.getEmail())
                .nickname(makeNickname())
                .providerType(oAuthResponse.getOAuthProvider())
                .providerId(oAuthResponse.getId())
                .build();

        return userRepository.save(user);
    }

    @Override
    public String makeNickname() {
        String nickname;
        do {
            String randomAdjective = getRandomItem(ADJECTIVES);
            String randomNoun = getRandomItem(NOUNS);
            nickname = randomAdjective + randomNoun;
        } while (userRepository.existsByNickname(nickname));
        return nickname;
    }

    @Override
    public UserDto update(UserInfo userInfo, UserDto userDto) {
        return userRepository.findByNickname(userInfo.getNickname())
                .filter(userEntity -> userDto.getEmail().equals(userEntity.getEmail()))
                .map(user -> {
                    user.setNickname(userDto.getNickname());
                    UserEntity savedUser = userRepository.save(user);
                    return UserDto.builder()
                            .id(savedUser.getId())
                            .name(savedUser.getName())
                            .nickname(savedUser.getNickname())
                            .email(savedUser.getEmail())
                            .providerType(savedUser.getProviderType())
                            .role(savedUser.getRole())
                            .build();
                })
                .orElse(null);
    }

    @Override
    public boolean delete(UserInfo userInfo, UserDto userDto) {
        return userRepository.findByNickname(userInfo.getNickname())
                .filter(userEntity -> userDto.getEmail().equals(userEntity.getEmail()))
                .map(user -> {
                    userRepository.delete(user);
                    return true;
                })
                .orElse(false);
    }

    private String getRandomItem(List<String> items) {
        Random random = new Random();
        int index = random.nextInt(items.size());
        return items.get(index);
    }

    @Override
    public boolean logout(OAuthProvider oAuthProvider, HttpSession session) {
        log.info("LogOut {}", oAuthProvider);
        boolean response = OAuthService.revoke(oAuthProvider, session);

        if (response) {
            session.invalidate();
        }
        return response;
    }

    private static final List<String> ADJECTIVES = List.of(
            "멋진", "행복한", "귀여운", "똑똑한", "친절한", "강한", "빠른", "정직한", "따뜻한", "용감한",
            "웃긴", "엉뚱한", "재미있는", "상큼한", "부드러운", "천진난만한", "귀찮은", "엉망인", "조용한", "핸드폰 중독된",
            "달콤한", "매운", "편안한", "짧은", "길쭉한", "날렵한", "어두운", "귀찮은", "반짝이는", "덥썩잡은", "호기심 많은",
            "그윽한", "낯선", "어리숙한", "영리한", "멍한", "불가사의한", "상상력이 풍부한", "미친", "흥미로운", "단순한",
            "상큼한", "촘촘한", "비밀스러운", "나른한", "자상한", "발랄한", "불편한", "정겨운", "부끄러운", "섹시한",
            "호쾌한", "넓은", "까칠한", "짧은", "몽환적인", "기분 좋은", "재치 있는", "발랄한", "깔끔한", "신비로운",
            "불편한", "쌈박한", "조급한", "지혜로운", "외로운", "평화로운", "야무진", "슬픈", "차분한", "두려운", "상처받은",
            "뻔한", "어리석은", "과장된", "대담한", "깔끔한", "졸린", "배고픈", "흥분한", "어색한", "단호한"
    );

    private static final List<String> NOUNS = List.of(
            "사람", "고양이", "친구", "강아지", "나무", "꽃", "별", "하늘", "바다", "산", "하이에나",
            "치타", "곰", "악어", "고래", "탱고", "파리", "장미", "햄스터", "너구리", "댕댕이", "사자",
            "호랑이", "토끼", "오리", "펭귄", "코끼리", "팬더", "수달", "악동", "괴물", "인형", "식물",
            "햇볕", "라면", "롤러코스터", "아기", "곰돌이", "양파", "눈송이", "타조", "너비", "드래곤",
            "고슴도치", "장난감", "도마뱀", "방울뱀", "귤", "사탕", "알파카", "닭", "카멜레온", "레서판다",
            "백합", "선인장", "고사리", "소나무", "국화", "백일홍", "장미꽃", "고추", "토마토", "상추",
            "양배추", "바질", "파슬리", "라벤더", "해바라기", "오이", "감자", "대나무", "대추나무", "포도",
            "미나리", "호박", "허브", "참외", "알로에", "귤나무", "카카두", "밀감", "귤"
    );
}

package kr.co.pinup.users.service.impl;

import jakarta.servlet.http.HttpSession;
import kr.co.pinup.users.oauth.OAuthLoginParams;
import kr.co.pinup.users.oauth.OAuthProvider;
import kr.co.pinup.users.oauth.OAuthResponse;
import kr.co.pinup.users.oauth.OAuthService;
import kr.co.pinup.users.model.UserDto;
import kr.co.pinup.users.model.UserEntity;
import kr.co.pinup.users.model.UserInfo;
import kr.co.pinup.users.model.UserRepository;
import kr.co.pinup.users.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final OAuthService oAuthService;

    @Override
    public UserInfo login(OAuthLoginParams params, HttpSession session) {
        try {
            OAuthResponse oAuthResponse = oAuthService.request(params, session);
            UserEntity user = findOrCreateUser(oAuthResponse);
            return UserInfo.builder()
                    .nickname(user.getNickname())
                    .provider(user.getProviderType())
                    .role(user.getRole())
                    .build();
        } catch (Exception e) {
            log.error("Login failed: {}", e.getMessage());
            throw new IllegalStateException("로그인 처리 중 문제가 발생했습니다.", e);
        }
    }

    private UserEntity findOrCreateUser(OAuthResponse oAuthResponse) {
        return userRepository.findByEmail(oAuthResponse.getEmail())
                .orElseGet(() -> newUser(oAuthResponse));
    }

    private UserEntity newUser(OAuthResponse oAuthResponse) {
        try {
            UserEntity user = UserEntity.builder()
                    .name(oAuthResponse.getName())
                    .email(oAuthResponse.getEmail())
                    .nickname(makeNickname())
                    .providerType(oAuthResponse.getOAuthProvider())
                    .providerId(oAuthResponse.getId())
                    .build();
            return userRepository.save(user);
        } catch (Exception e) {
            log.error("Error creating new user: {}", e.getMessage());
            throw new RuntimeException("사용자 생성 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public String makeNickname() {
        String nickname;
        try {
            do {
                String randomAdjective = getRandomItem(ADJECTIVES);
                String randomNoun = getRandomItem(NOUNS);
                nickname = randomAdjective + randomNoun;
            } while (userRepository.existsByNickname(nickname));
            return nickname;
        } catch (Exception e) {
            log.error("Error generating nickname: {}", e.getMessage());
            throw new RuntimeException("닉네임 생성 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public UserDto findUser(UserInfo userInfo) {
        try {
            return userRepository.findByNickname(userInfo.getNickname())
                    .map(userEntity -> UserDto.builder()
                            .name(userEntity.getName())
                            .email(userEntity.getEmail())
                            .nickname(userEntity.getNickname())
                            .providerType(userEntity.getProviderType())
                            .role(userEntity.getRole())
                            .build())
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        } catch (Exception e) {
            log.error("Error finding user: {}", e.getMessage());
            throw new RuntimeException("사용자 조회 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public UserDto update(UserInfo userInfo, UserDto userDto) {
        try {
            UserEntity userEntity = userRepository.findByNickname(userInfo.getNickname())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

            if (!userEntity.getEmail().equals(userDto.getEmail())) {
                throw new IllegalArgumentException("이메일이 일치하지 않습니다.");
            }

            if (userRepository.findByNickname(userDto.getNickname()).isPresent()) {
                throw new IllegalArgumentException("중복된 닉네임입니다.");
            }

            userEntity.setNickname(userDto.getNickname());
            UserEntity updatedUser = userRepository.save(userEntity);

            return UserDto.builder()
                    .id(updatedUser.getId())
                    .name(updatedUser.getName())
                    .nickname(updatedUser.getNickname())
                    .email(updatedUser.getEmail())
                    .providerType(updatedUser.getProviderType())
                    .role(updatedUser.getRole())
                    .build();
        } catch (IllegalArgumentException e) {
            log.error("Duplicate nickname error: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Error updating user: {}", e.getMessage());
            throw new RuntimeException("사용자 업데이트 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public boolean delete(UserInfo userInfo, UserDto userDto) {
        try {
            return userRepository.findByNickname(userInfo.getNickname())
                    .filter(userEntity -> userDto.getEmail().equals(userEntity.getEmail()))
                    .map(user -> {
                        userRepository.delete(user);
                        return true;
                    })
                    .orElse(false);
        } catch (Exception e) {
            log.error("Error deleting user: {}", e.getMessage());
            throw new RuntimeException("사용자 삭제 중 문제가 발생했습니다.", e);
        }
    }

    private String getRandomItem(List<String> items) {
        try {
            Random random = new Random();
            int index = random.nextInt(items.size());
            return items.get(index);
        } catch (Exception e) {
            log.error("Error getting random item: {}", e.getMessage());
            throw new RuntimeException("랜덤 항목 선택 중 문제가 발생했습니다.", e);
        }
    }

    @Override
    public boolean logout(OAuthProvider oAuthProvider, HttpSession session) {
        try {
            log.info("LogOut {}", oAuthProvider);
            boolean response = oAuthService.revoke(oAuthProvider, session);

            if (response) {
                session.invalidate();
            }
            return response;
        } catch (Exception e) {
            log.error("Error logging out: {}", e.getMessage());
            throw new RuntimeException("로그아웃 처리 중 문제가 발생했습니다.", e);
        }
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

package kr.co.pinup.members.service;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.MemberNotFoundException;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.OAuthLoginParams;
import kr.co.pinup.oauth.OAuthProvider;
import kr.co.pinup.oauth.OAuthResponse;
import kr.co.pinup.oauth.OAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final OAuthService oAuthService;

    public MemberInfo login(OAuthLoginParams params, HttpSession session) {
        OAuthResponse oAuthResponse = oAuthService.request(params, session);
        Member user = findOrCreateUser(oAuthResponse);
        return MemberInfo.builder()
                .nickname(user.getNickname())
                .provider(user.getProviderType())
                .role(user.getRole())
                .build();
    }

    private Member findOrCreateUser(OAuthResponse oAuthResponse) {
        return memberRepository.findByEmail(oAuthResponse.getEmail())
                .orElseGet(() -> newUser(oAuthResponse));
    }

    private Member newUser(OAuthResponse oAuthResponse) {
        Member user = Member.builder()
                .name(oAuthResponse.getName())
                .email(oAuthResponse.getEmail())
                .nickname(makeNickname())
                .providerType(oAuthResponse.getOAuthProvider())
                .providerId(oAuthResponse.getId())
                .build();

        return memberRepository.save(user);
    }

    public String makeNickname() {
        String nickname;
        do {
            String randomAdjective = getRandomItem(ADJECTIVES);
            String randomNoun = getRandomItem(NOUNS);
            nickname = randomAdjective + randomNoun;
        } while (memberRepository.existsByNickname(nickname));
        return nickname;
    }

    public MemberResponse findUser(MemberInfo memberInfo) {
        try {
            return memberRepository.findByNickname(memberInfo.getNickname())
                    .map(member -> new MemberResponse(member))
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        } catch (IllegalArgumentException e) {
            log.error("Error while fetching user: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while fetching user: {}", e.getMessage());
            throw new RuntimeException("서버 오류가 발생했습니다.");
        }
    }

    public MemberResponse update(MemberInfo memberInfo, MemberRequest memberRequest) {
        if (memberRequest.getEmail() == null) {
            throw new IllegalArgumentException("이메일은 null일 수 없습니다.");
        }

        Member member = memberRepository.findByNickname(memberInfo.getNickname())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        if (!memberRequest.getEmail().equals(member.getEmail())) {
            throw new IllegalArgumentException("이메일이 일치하지 않습니다.");
        }

        if (memberRepository.findByNickname(memberRequest.getNickname()).isPresent()) {
            throw new IllegalArgumentException("\""+ memberRequest.getNickname()+"\"은 중복된 닉네임입니다.");
        }

        member.setNickname(memberRequest.getNickname());
        Member savedUser = memberRepository.save(member);

        return new MemberResponse(savedUser);
    }

    public boolean delete(MemberInfo memberInfo, MemberRequest memberRequest) {
        try {
            Optional<Member> userEntityOptional = memberRepository.findByNickname(memberInfo.getNickname());

            if (userEntityOptional.isPresent()) {
                Member member = userEntityOptional.get();
                if (memberRequest.getEmail().equals(member.getEmail())) {
                    memberRepository.delete(member);
                    return true;
                } else {
                    throw new UnauthorizedException("권한이 없습니다.");
                }
            } else {
                throw new MemberNotFoundException("사용자를 찾을 수 없습니다.");
            }
        } catch (Exception e) {
            throw new RuntimeException("An error occurred while deleting the user: " + e.getMessage(), e);
        }
    }

    private String getRandomItem(List<String> items) {
        Random random = new Random();
        int index = random.nextInt(items.size());
        return items.get(index);
    }

    public boolean logout(OAuthProvider oAuthProvider, HttpSession session) {
        log.info("LogOut {}", oAuthProvider);
        boolean response = oAuthService.revoke(oAuthProvider, session);

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

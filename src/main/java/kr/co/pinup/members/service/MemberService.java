package kr.co.pinup.members.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.*;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
        Member member = findOrCreateMember(oAuthResponse);
        MemberInfo memberInfo = MemberInfo.builder()
                .nickname(member.getNickname())
                .provider(member.getProviderType())
                .role(member.getRole())
                .build();

        // SecurityContext에 저장
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(memberInfo, null, memberInfo.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authentication);
        // 현재 jwt를 사용하지 않고 세션 기반으로 인증하고 있기 때문에 SecurityContext 자체를 저장해야함
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        return memberInfo;
    }

    private Member findOrCreateMember(OAuthResponse oAuthResponse) {
        return memberRepository.findByEmail(oAuthResponse.getEmail())
                .orElseGet(() -> newMember(oAuthResponse));
    }

    private Member newMember(OAuthResponse oAuthResponse) {
        Member member = Member.builder()
                .name(oAuthResponse.getName())
                .email(oAuthResponse.getEmail())
                .nickname(makeNickname())
                .providerType(oAuthResponse.getOAuthProvider())
                .providerId(oAuthResponse.getId())
                .build();

        return memberRepository.save(member);
    }

    public String makeNickname() {
        String nickname;
        do {
            String randomAdjective = getRandomItem(ADJECTIVES);
            String randomNoun = getRandomItem(NOUNS);
            nickname = randomAdjective + " " + randomNoun;
        } while (memberRepository.existsByNickname(nickname));
        return nickname;
    }

    public MemberResponse findMember(MemberInfo memberInfo) {
        return memberRepository.findByNickname(memberInfo.nickname())
                .map(MemberResponse::new)
                .orElseThrow(MemberNotFoundException::new);
    }

    public MemberResponse update(MemberInfo memberInfo, MemberRequest memberRequest) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException());

        if (!memberRequest.email().equals(member.getEmail())) {
            throw new MemberBadRequestException("이메일이 일치하지 않습니다.");
        }

        if (memberRepository.findByNickname(memberRequest.nickname()).isPresent()) {
            throw new MemberBadRequestException("\"" + memberRequest.nickname() + "\"은 중복된 닉네임입니다.");
        }

        try {
            member.setNickname(memberRequest.nickname());
            Member savedMember = memberRepository.save(member);

            MemberInfo updatedMemberInfo = MemberInfo.builder()
                    .nickname(savedMember.getNickname())
                    .provider(memberInfo.provider())
                    .role(memberInfo.role())
                    .build();

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(updatedMemberInfo, null, updatedMemberInfo.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            return new MemberResponse(savedMember);
        } catch (DataIntegrityViolationException e) {
            throw new MemberServiceException("회원 정보 저장 중 제약 조건 위반이 발생했습니다.");
        } catch (Exception e) {
            throw new MemberServiceException("회원 정보 저장 중 오류가 발생했습니다.");
        }
    }

    public boolean delete(MemberInfo memberInfo, MemberRequest memberRequest) {
        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> new MemberNotFoundException());

        if (!memberRequest.email().equals(member.getEmail())) {
            throw new UnauthorizedException("권한이 없습니다.");
        }

        try {
            memberRepository.delete(member);
        } catch (Exception e) {
            throw new MemberServiceException("회원 삭제 중 오류가 발생했습니다.");
        }

        return true;
    }

    public boolean logout(OAuthProvider oAuthProvider, HttpServletRequest request) {
        log.info("Logging out with provider {}", oAuthProvider);
        if (oAuthProvider == null) {
            throw new OAuthProviderNotFoundException("OAuth 제공자가 없습니다.");
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            throw new UnauthorizedException("세션이 존재하지 않습니다. 다시 로그인 해주세요.");
        }

        String accessToken = (String) session.getAttribute("accessToken");
        if (accessToken == null || accessToken.isEmpty()) {
            throw new OAuthTokenNotFoundException("OAuth 토큰을 찾을 수 없습니다.");
        }

        boolean response = oAuthService.revoke(oAuthProvider, accessToken);
        if (!response) {
            throw new OAuth2AuthenticationException("OAuth 로그아웃 중 오류가 발생했습니다.");
        }

        SecurityContextHolder.clearContext();
        session.invalidate();

        return true;
    }

    private String getRandomItem(List<String> items) {
        Random random = new Random();
        int index = random.nextInt(items.size());
        return items.get(index);
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

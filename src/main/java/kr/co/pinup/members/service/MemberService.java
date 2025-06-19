package kr.co.pinup.members.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.custom.logging.model.dto.ErrorLog;
import kr.co.pinup.custom.logging.model.dto.InfoLog;
import kr.co.pinup.custom.logging.model.dto.WarnLog;
import kr.co.pinup.exception.common.UnauthorizedException;
import kr.co.pinup.members.Member;
import kr.co.pinup.members.exception.*;
import kr.co.pinup.members.model.dto.MemberInfo;
import kr.co.pinup.members.model.dto.MemberRequest;
import kr.co.pinup.members.model.dto.MemberResponse;
import kr.co.pinup.members.repository.MemberRepository;
import kr.co.pinup.oauth.*;
import kr.co.pinup.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;

@Service
@Transactional
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final OAuthService oAuthService;
    private final SecurityUtil securityUtil;
    private final AppLogger appLogger;

    public Triple<OAuthResponse, OAuthToken, String> login(OAuthLoginParams params, HttpSession session) {
        appLogger.info(new InfoLog("OAuth Login 시작 - provider: " + params.oAuthProvider()));

        Pair<OAuthResponse, OAuthToken> oAuthResponseOAuthTokenPair = oAuthService.request(params);
        OAuthToken oAuthToken = oAuthResponseOAuthTokenPair.getRight();
        OAuthResponse oAuthResponse = oAuthResponseOAuthTokenPair.getLeft();

        if (oAuthToken == null) {
            appLogger.warn(new WarnLog("OAuth 서버로부터 access token을 받지 못함").setStatus("401"));
            throw new UnauthorizedException("MemberService : OAuth token is null");
        }

        if (oAuthResponse == null) {
            appLogger.warn(new WarnLog("OAuth 서버로부터 사용자 정보를 받지 못함").setStatus("401"));
            throw new UnauthorizedException("MemberService : OAuth response is null");
        }

        appLogger.info(new InfoLog("OAuth 인증 성공 - provider: " + params.oAuthProvider()));

        appLogger.info(new InfoLog("OAuth 인증 성공 - email: " + oAuthResponse.getEmail() + ", name: " + oAuthResponse.getName()));

        Pair<Member, String> memberStringPair = findOrCreateMember(oAuthResponse);
        Member member = memberStringPair.getLeft();

        if (member == null) {
            appLogger.warn(new WarnLog("OAuth로 사용자 정보를 받아왔으나 DB에 저장/조회 실패").setStatus("500"));
            throw new MemberServiceException("회원 조회 실패");
        }

        appLogger.info(new InfoLog("OAuth Login 성공 - nickname: " + member.getNickname() + ", role: " + member.getRole()));

        MemberInfo memberInfo = MemberInfo.builder()
                .nickname(member.getNickname())
                .provider(member.getProviderType())
                .role(member.getRole())
                .build();

        securityUtil.setAuthentication(oAuthToken, memberInfo);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, SecurityContextHolder.getContext());

        return Triple.ofNonNull(oAuthResponse, oAuthToken, memberStringPair.getRight());
    }

    private Pair<Member, String> findOrCreateMember(OAuthResponse oAuthResponse) {
        Optional<Member> optionalMember = memberRepository.findByEmailAndIsDeletedFalse(oAuthResponse.getEmail());

        if (optionalMember.isPresent()) {
            Member member = optionalMember.get();
            appLogger.info(new InfoLog("기존 회원 로그인: email='" + member.getEmail() + "', nickname='" + member.getNickname() + "'"));
            return Pair.ofNonNull(member, "다시 돌아오신 걸 환영합니다 \"" + member.getName() + "\"님");
        } else {
            appLogger.info(new InfoLog("신규 회원 등록: email='" + oAuthResponse.getEmail()));
            return newMember(oAuthResponse);
        }
    }

    private Pair<Member, String> newMember(OAuthResponse oAuthResponse) {
        Member member = Member.builder()
                .name(oAuthResponse.getName())
                .email(oAuthResponse.getEmail())
                .nickname(makeNickname())
                .providerType(oAuthResponse.getOAuthProvider())
                .providerId(oAuthResponse.getId())
                .build();

        return Pair.ofNonNull(memberRepository.save(member), "환영합니다 \"" + member.getNickname() + "\"님");
    }

    public String makeNickname() {
        String nickname;
        int tryCount = 0;

        do {
            nickname = getRandomItem(ADJECTIVES) + " " + getRandomItem(NOUNS);
            tryCount++;
        } while (memberRepository.existsByNickname(nickname));

        appLogger.info(new InfoLog("닉네임 생성 - nickname: " + nickname + ", 시도 횟수: " + tryCount));
        return nickname;
    }

    public MemberResponse findMember(MemberInfo memberInfo) {
        return memberRepository.findByNickname(memberInfo.nickname())
                .map(MemberResponse::new)
                .orElseThrow(MemberNotFoundException::new);
    }

    public MemberResponse update(MemberInfo memberInfo, MemberRequest memberRequest) {
        appLogger.info(new InfoLog("회원 정보 수정 요청 - nickname: " + memberInfo.nickname()));

        Member member = memberRepository.findByNickname(memberInfo.nickname())
                .orElseThrow(() -> {
                    appLogger.warn(new WarnLog("회원 정보 수정 실패 - 사용자를 찾을 수 없음")
                            .setStatus("404"));
                    return new MemberNotFoundException();
                });

        if (!memberRequest.email().equals(member.getEmail())) {
            appLogger.warn(new WarnLog("회원 정보 수정 실패 - 이메일 불일치: 요청=" + memberRequest.email() + ", DB=" + member.getEmail()).setStatus("400"));
            throw new MemberBadRequestException("이메일이 일치하지 않습니다.");
        }

        if (memberRepository.findByNickname(memberRequest.nickname()).isPresent()) {
            appLogger.warn(new WarnLog("회원 정보 수정 실패 - 닉네임 중복: " + memberRequest.nickname())
                    .setStatus("400"));
            throw new MemberBadRequestException("\"" + memberRequest.nickname() + "\"은 중복된 닉네임입니다.");
        }

        if (memberRequest.nickname().length() > 50) {
            appLogger.warn(new WarnLog("회원 정보 수정 실패 - 닉네임 길이 초과: " + memberRequest.nickname()).setStatus("400"));
            throw new MemberBadRequestException("닉네임은 최대 50자입니다.");
        }

        try {
            member.setNickname(memberRequest.nickname());
            Member savedMember = memberRepository.save(member);

            appLogger.info(new InfoLog("회원 정보 수정 성공 - newNickname: " + savedMember.getNickname()));

            MemberInfo updatedMemberInfo = MemberInfo.builder()
                    .nickname(savedMember.getNickname())
                    .provider(memberInfo.provider())
                    .role(memberInfo.role())
                    .build();

            securityUtil.setMemberInfo(updatedMemberInfo);

            return MemberResponse.fromMember(savedMember);
        } catch (DataIntegrityViolationException e) {
            appLogger.warn(new WarnLog("회원 정보 수정 실패 - 제약 조건 위반").setStatus("500"));
            throw new MemberServiceException("회원 정보 저장 중 제약 조건 위반이 발생했습니다.");
        } catch (Exception e) {
            appLogger.error(new ErrorLog("회원 정보 수정 실패 - 알 수 없는 오류", e).setStatus("500"));
            throw new MemberServiceException("회원 정보 저장 중 오류가 발생했습니다.");
        }
    }

    public boolean disable(MemberInfo memberInfo, MemberRequest memberRequest) {
        String nickname = memberInfo.nickname();
        appLogger.info(new InfoLog("회원 탈퇴 요청: nickname = " + nickname));

        Member member = memberRepository.findByNickname(nickname)
                .orElseThrow(() -> {
                    appLogger.warn(new WarnLog("회원 닉네임 '" + nickname + "' 을(를) 찾을 수 없음").setStatus("404"));
                    return new MemberNotFoundException();
                });

        if (!memberRequest.email().equals(member.getEmail())) {
            appLogger.warn(new WarnLog("이메일 불일치: 요청 이메일 = " + memberRequest.email() + ", 실제 이메일 = " + member.getEmail()).setStatus("401"));
            throw new UnauthorizedException("권한이 없습니다.");
        }

        try {
            memberRepository.updateIsDeletedTrue(member.getId());
            appLogger.info(new InfoLog("회원 탈퇴 처리 완료: id = " + member.getId()));

            securityUtil.clearContextAndDeleteCookie();
            appLogger.info(new InfoLog("시큐리티 컨텍스트 및 쿠키 삭제 완료"));

            return true;
        } catch (Exception e) {
            appLogger.error(new ErrorLog("회원 탈퇴 실패 - 알 수 없는 오류", e).setStatus("500"));
            throw new MemberServiceException("회원 탈퇴 중 오류가 발생했습니다.");
        }
    }

    public boolean logout(OAuthProvider oAuthProvider, String accessToken) {
        appLogger.info(new InfoLog("로그아웃 요청 - provider: " + oAuthProvider + ", accessToken 존재 여부: " + (accessToken != null)));

        if (oAuthProvider == null) {
            appLogger.warn(new WarnLog("로그아웃 실패 - OAuth 제공자 누락").setStatus("400"));
            throw new OAuthProviderNotFoundException("OAuth 제공자가 없습니다.");
        }

        if (accessToken == null || accessToken.isEmpty()) {
            appLogger.warn(new WarnLog("로그아웃 실패 - Access Token 누락").setStatus("401"));
            throw new OAuthTokenNotFoundException("MemberService logout || OAuth Access 토큰을 찾을 수 없습니다.");
        }

        try {
            securityUtil.clearContextAndDeleteCookie();
            appLogger.info(new InfoLog("로그아웃 성공 - provider: " + oAuthProvider));
        } catch (OAuthTokenRequestException e) {
            appLogger.warn(new WarnLog("로그아웃 실패 - OAuth 토큰 요청 오류").setStatus("500"));
            throw new OAuth2AuthenticationException();
        } catch (Exception e) {
            appLogger.error(new ErrorLog("로그아웃 실패 - 알 수 없는 오류", e).setStatus("500"));
            throw new MemberServiceException("로그아웃 중 오류가 발생했습니다.");
        }

        return true;
    }

    public boolean isAccessTokenExpired(MemberInfo memberInfo, String accessToken) {
        appLogger.info(new InfoLog("AccessToken 만료 여부 확인 시작: provider = " + memberInfo.provider() + ", nickname = " + memberInfo.nickname()));

        try {
            OAuthResponse oAuthResponse = oAuthService.isAccessTokenExpired(memberInfo.provider(), accessToken);

            if (oAuthResponse != null) {
                appLogger.info(new InfoLog("OAuth 응답 수신: email = " + oAuthResponse.getEmail()));

                Optional<Member> memberOpt = memberRepository.findByEmailAndIsDeletedFalse(oAuthResponse.getEmail());

                if (memberOpt.isPresent()) {
                    Member member = memberOpt.get();
                    MemberInfo foundInfo = MemberInfo.builder()
                            .nickname(member.getNickname())
                            .provider(member.getProviderType())
                            .role(member.getRole())
                            .build();

                    if (!foundInfo.equals(memberInfo)) {
                        appLogger.warn(new WarnLog("AccessToken은 유효하나, 사용자 정보 불일치: 요청 = " + memberInfo + ", 실제 = " + foundInfo));
                        return true;
                    }

                    appLogger.info(new InfoLog("AccessToken 유효 및 사용자 일치 확인 완료"));
                } else {
                    appLogger.warn(new WarnLog("해당 이메일로 활성화된 회원 없음: email = " + oAuthResponse.getEmail()));
                }
            } else {
                appLogger.warn(new WarnLog("OAuth 응답이 없습니다"));
            }

            return false;
        } catch (OAuthAccessTokenNotFoundException e) {
            appLogger.error(new ErrorLog("AccessToken를 찾을 수 없습니다.", e));
            return true;
        } catch (Exception e) {
            appLogger.error(new ErrorLog("AccessToken 만료 확인 실패 - 알 수 없는 오류", e));
            return true;
        }
    }

    public String refreshAccessToken(HttpServletRequest request) {
        MemberInfo memberInfo = securityUtil.getMemberInfo();
        appLogger.info(new InfoLog("AccessToken 재발급 요청: provider = " + memberInfo.getProvider() + ", nickname = " + memberInfo.nickname()));

        String refreshToken = securityUtil.getOptionalRefreshToken(request);

        if (refreshToken == null) {
            appLogger.warn(new WarnLog("AccessToken 재발급 실패 - Refresh token이 존재하지 않습니다.").setStatus("401"));
            throw new OAuthTokenNotFoundException("Refresh token이 존재하지 않습니다.");
        }

        try {
            OAuthToken token = oAuthService.refresh(memberInfo.getProvider(), refreshToken);
            appLogger.info(new InfoLog("AccessToken 재발급 성공: provider = " + memberInfo.getProvider()));

            securityUtil.refreshAccessTokenInSecurityContext(token.getAccessToken());
            return token.getAccessToken();
        } catch (Exception e) {
            appLogger.error(new ErrorLog("AccessToken 재발급 실패 - 알 수 없는 오류", e));
            throw new UnauthorizedException("AccessToken 재발급에 실패했습니다.");
        }
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

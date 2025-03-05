package kr.co.pinup.security;

import java.util.List;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/images/**", "/members/login", "/api/members/oauth/**",
            "/stores", "/api/stores", "/api/stores/summary",
            "/post",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs"
    };

    // 필터 적용 제외할 URL 목록
    public static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/stores", "/api/stores", "/api/stores/summary",
            "/post",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs"
    );
}

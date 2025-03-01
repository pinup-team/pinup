package kr.co.pinup.security;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/images/**", "/members/login", "/api/members/oauth/**",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/stores", "/api/stores", "/api/stores/summary",
            "/post", "/faqs"
    };
}

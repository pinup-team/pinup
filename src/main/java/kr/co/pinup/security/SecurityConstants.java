package kr.co.pinup.security;

import java.util.List;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/error", "/images/**", "/terms-privacy/**",
            "/members/login", "/api/members/oauth/**",
            "/stores", "/stores/{id}", "/api/stores", "/api/stores/summary", "/api/stores/{id}",
            "/post", "/api/post/list/{storeId}", "/api/post/{postId}", "/post/list/{storeId}", "/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    };

    // 필터 적용 제외할 URL 목록
    public static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/terms-privacy/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/stores", "/stores/{id}", "/api/stores", "/api/stores/summary", "/api/stores/{id}",
            "/post", "/api/post/list/{storeId}", "/api/post/{postId}", "/post/list/{storeId}", "/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    );
}

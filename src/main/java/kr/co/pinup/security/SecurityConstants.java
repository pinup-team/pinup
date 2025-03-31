package kr.co.pinup.security;

import java.util.List;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/static/**", "/templates/**", "/css/**", "/js/**", "/images/**", "/terms-privacy/**", "/fonts/**", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/**",
            "/stores", "/stores/{storeId:[0-9]+}", "/api/stores", "/api/stores/summary", "/api/stores/{storeId:[0-9]+}",
            "/post", "/post/{postId:[0-9]+}", "/post/list/{postId:[0-9]+}", "/api/post/list/{storeId}", "/api/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    };

    // 필터 적용 제외할 URL 목록
    public static final List<String> EXCLUDED_URLS = List.of(
            "/", "/static/", "/templates/", "/css/", "/js/", "/images/", "/terms-privacy/", "/fonts/", "/error", "/favicon.ico",
            "/members/login", "/api/members/oauth/",
            "/stores", "/stores/{id}", "/api/stores", "/api/stores/summary", "/api/stores/{id}",
            "/post", "/post/{postId}", "/post/list/{storeId}", "/api/post/list/{storeId}", "/api/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    );
}

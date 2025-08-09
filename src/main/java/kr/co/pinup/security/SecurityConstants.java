package kr.co.pinup.security;

import java.util.List;

public class SecurityConstants {
    public static final String[] PUBLIC_URLS = {
            "/", "/static/**", "/templates/**", "/css/**", "/js/**", "/images/**", "/docs/**", "/terms-privacy/**", "/fonts/**", "/error", "/favicon.ico", "/.well-known/appspecific/com.chrome.devtools.json",
            "/members/login", "/members/register", "/api/members/oauth/**", "/api/members/login", "/api/members/validate", "/api/members/register", "/api/members/nickname",
            "/stores", "/stores/{storeId:[0-9]+}", "/api/stores", "/api/stores/summary", "/api/stores/{storeId:[0-9]+}",
            "/post", "/post/{postId:[0-9]+}", "/post/list/{postId:[0-9]+}", "/api/post/list/{storeId}", "/api/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    };

    // 필터 적용 제외할 URL 목록
    public static final List<String> EXCLUDED_URLS = List.of(
            "/static/", "/templates/", "/css/", "/js/", "/images/", "/docs/", "/terms-privacy/", "/fonts/", "/error", "/favicon.ico", "/.well-known/appspecific/com.chrome.devtools.json",
            "/members/login", "/members/register", "/api/members/oauth/", "/api/members/login", "/api/members/validate", "/api/members/register", "/api/members/nickname",
            "/stores", "/stores/{id}", "/api/stores", "/api/stores/summary", "/api/stores/{id}",
            "/post", "/post/{postId}", "/post/list/{storeId}", "/api/post/list/{storeId}", "/api/post/{postId}",
            "/notices", "/notices/{noticeId}", "/api/notices", "/api/notices/{noticeId}",
            "/faqs", "/api/faqs", "/api/faqs/{faqsId}"
    );
}

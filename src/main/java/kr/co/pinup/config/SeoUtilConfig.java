package kr.co.pinup.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SeoUtilConfig {

    @Value("${app.domain:}")
    private String baseDomain;

    public String getCanonicalUrl(HttpServletRequest request) {

        if (request == null) {
            return baseDomain;
        }

        StringBuilder url = new StringBuilder(baseDomain + request.getRequestURI());

        // TODO: 추후 쿼리스트링 포함 로직 필요 시 아래 코드로 확장
        String query = request.getQueryString();
        if (query != null && !query.isBlank()) {
            url.append("?").append(query);
        }
        return url.toString();
    }
    public String getBaseDomain() {
        return baseDomain;
    }
}

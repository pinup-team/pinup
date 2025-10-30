package kr.co.pinup.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix =  "spring.cache.app")
public record AppCacheProps(
        Defaults defaults,
        Map<String, Spec> caches
) {
    public record Defaults(Long maximumSize, Integer ttlSec) {}
    public record Spec(Long maximumSize, Integer ttlSec) {}
}
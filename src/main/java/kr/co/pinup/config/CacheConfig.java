package kr.co.pinup.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import kr.co.pinup.cache.AppCacheProps;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppCacheProps.class)
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(AppCacheProps props) {
        // per-cache 정책을 주입하려면 익명 클래스로 createCaffeineCache를 오버라이드
        return new CaffeineCacheManager() {
            {
                // strict: 선언되지 않은 캐시는 사용 불가
                setAllowNullValues(false);
                setCacheNames(props.caches().keySet());
            }

            @Override
            protected CaffeineCache createCaffeineCache(String name) {
                Caffeine<Object, Object> builder = Caffeine.newBuilder().recordStats();

                var d = props.defaults();        // 기본 정책
                var s = props.caches().get(name); // 캐시별 오버라이드

                // 1) 유효값을 먼저 계산(오버라이드 우선)
                Long maxSize = (s != null && s.maximumSize() != null)
                        ? s.maximumSize()
                        : (d != null ? d.maximumSize() : null);

                Integer ttlSec = (s != null && s.ttlSec() != null)
                        ? s.ttlSec()
                        : (d != null ? d.ttlSec() : null);

                // 2) 계산된 값만 "한 번씩" 세팅
                if (maxSize != null) {
                    builder = builder.maximumSize(maxSize);
                }
                if (ttlSec != null) {
                    builder = builder.expireAfterWrite(java.time.Duration.ofSeconds(ttlSec));
                }

                return new org.springframework.cache.caffeine.CaffeineCache(name, builder.build(), false);
            }

        };
    }
}

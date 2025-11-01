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

        return new CaffeineCacheManager() {
            {
                setAllowNullValues(false);
                setCacheNames(props.caches().keySet());
            }

            @Override
            protected CaffeineCache createCaffeineCache(String name) {
                Caffeine<Object, Object> builder = Caffeine.newBuilder().recordStats();

                var d = props.defaults();
                var s = props.caches().get(name);

                Long maxSize = (s != null && s.maximumSize() != null)
                        ? s.maximumSize()
                        : (d != null ? d.maximumSize() : null);

                Integer ttlSec = (s != null && s.ttlSec() != null)
                        ? s.ttlSec()
                        : (d != null ? d.ttlSec() : null);

                if (maxSize != null) builder = builder.maximumSize(maxSize);
                if (ttlSec != null) builder = builder.expireAfterWrite(java.time.Duration.ofSeconds(ttlSec));

                return new CaffeineCache(name, builder.build(), false);
            }

        };
    }
}

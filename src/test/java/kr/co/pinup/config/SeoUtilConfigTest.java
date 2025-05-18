package kr.co.pinup.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class SeoUtilConfigTest {

    @Bean
    public SeoUtilConfig seoUtilConfig() {
        return mock(SeoUtilConfig.class);
    }
}

package kr.co.pinup.util;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

@TestConfiguration
@Profile("test")
public class TestSecretsFetcherConfig {
    @Bean
    public SecretsFetcher secretsFetcher() {
        return new SecretsFetcher("http://localhost:4566/", "us-east-1", "test/api/kakaomap") {
            @Override
            public String getSecretField(String fieldName) {
                return "dummy-test-key";
            }
        };
    }
}

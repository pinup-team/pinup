package kr.co.pinup.config;

import kr.co.pinup.custom.logging.AppLogger;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class LoggerConfig {
    @Bean
    @Primary
    public AppLogger testAppLogger() {
        return Mockito.mock(AppLogger.class);
    }
}

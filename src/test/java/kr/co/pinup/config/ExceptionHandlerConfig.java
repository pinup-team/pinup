package kr.co.pinup.config;

import kr.co.pinup.custom.logging.AppLogger;
import kr.co.pinup.exception.GlobalExceptionHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

@TestConfiguration
public class ExceptionHandlerConfig {

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public GlobalExceptionHandler globalExceptionHandler(final AppLogger appLogger) {
        return new GlobalExceptionHandler(appLogger);
    }
}

package kr.co.pinup.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.function.Supplier;

@Configuration
public class SchedulerConfig {

    private static final String ASIA_SEOUL_ZONE = "Asia/Seoul";

    @Bean
    public Supplier<LocalDate> todaySupplier() {
        return () -> LocalDate.now(ZoneId.of(ASIA_SEOUL_ZONE));
    }
}

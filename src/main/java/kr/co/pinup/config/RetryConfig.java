package kr.co.pinup.config;

import jakarta.persistence.OptimisticLockException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.orm.jpa.JpaOptimisticLockingFailureException;
import org.springframework.retry.backoff.ExponentialRandomBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

@Configuration
public class RetryConfig {

    @Bean
    public RetryTemplate retryTemplate() {
        RetryTemplate retryTemplate = new RetryTemplate();

        SimpleRetryPolicy retryPolicy = new SimpleRetryPolicy(
                6,
                Map.of(
                        ObjectOptimisticLockingFailureException.class, true,
                        JpaOptimisticLockingFailureException.class, true,
                        OptimisticLockException.class, true,
                        RuntimeException.class, true
                ),
                true
        );

        ExponentialRandomBackOffPolicy backOffPolicy = new ExponentialRandomBackOffPolicy ();
        backOffPolicy.setInitialInterval(200);
        backOffPolicy.setMultiplier(1.8);
        backOffPolicy.setMaxInterval(2500);

        retryTemplate.setRetryPolicy(retryPolicy);
        retryTemplate.setBackOffPolicy(backOffPolicy);
        return retryTemplate;
    }

    @Bean
    public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {
        return new TransactionTemplate(transactionManager);
    }
}

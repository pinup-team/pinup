package kr.co.pinup.postLikes;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.Callable;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostLikeRetryExecutor {

    private final RetryTemplate retryTemplate;
    private final TransactionTemplate transactionTemplate;

    public <T> T likeWithRetry(Callable<T> action) {
        return retryTemplate.execute(context -> {
            log.debug("📌 재시도 시작 (attempt {})", context.getRetryCount());

            try {
                return transactionTemplate.execute(status -> {
                    try {
                        return action.call();
                    }  catch (Exception e) {
                        if (e instanceof RuntimeException) throw (RuntimeException) e;
                        throw new RuntimeException(e);
                    }
                });
            } catch (Exception e) {
                log.warn("❌ 재시도 실패 (attempt {}): {}", context.getRetryCount(), e.getMessage());
                throw e;
            }
        });
    }
}
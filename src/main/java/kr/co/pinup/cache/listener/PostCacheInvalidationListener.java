package kr.co.pinup.cache.listener;

import kr.co.pinup.cache.CacheNames;
import kr.co.pinup.posts.event.PostCacheEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PostCacheEvent event) {
        switch (event.kind()) {
            case UPDATED -> {
                if (event.detailChanged()) {
                    evictIfPresent(CacheNames.POST_DETAIL, event.postId());
                }
                if (event.imagesChanged()) {
                    evictIfPresent(CacheNames.POST_IMAGES, event.postId());
                }
            }
            case DISABLED -> {
                evictIfPresent(CacheNames.POST_DETAIL, event.postId());
            }
            case DELETED -> {
                evictIfPresent(CacheNames.POST_DETAIL, event.postId());
                evictIfPresent(CacheNames.POST_IMAGES, event.postId());
            }
        }
    }

    private void evictIfPresent(String cacheName, Object key) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.evict(key);
        }
    }
}

package kr.co.pinup.cache.listener;

import kr.co.pinup.cache.CacheNames;
import kr.co.pinup.posts.event.PostCacheEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class PostCacheInvalidationListener {

    private final CacheManager cacheManager;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PostCacheEvent e) {
        switch (e.kind()) {
            case UPDATED -> {
                if (e.detailChanged()) {
                    var d = cacheManager.getCache(CacheNames.POST_DETAIL);
                    if (d != null) d.evict(e.postId());
                }
                if (e.imagesChanged()) {
                    var i = cacheManager.getCache(CacheNames.POST_IMAGES);
                    if (i != null) i.evict(e.postId());
                }
            }
            case DISABLED -> {
                var d = cacheManager.getCache(CacheNames.POST_DETAIL);
                if (d != null) d.evict(e.postId());
            }
            case DELETED -> {
                var d = cacheManager.getCache(CacheNames.POST_DETAIL);
                if (d != null) d.evict(e.postId());
                var i = cacheManager.getCache(CacheNames.POST_IMAGES);
                if (i != null) i.evict(e.postId());
            }
        }
    }
}
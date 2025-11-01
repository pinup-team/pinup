package kr.co.pinup.posts.event;

public record PostCacheEvent(
        Long postId,
        Kind kind,
        boolean detailChanged,   // UPDATED일 때만 의미
        boolean imagesChanged    // UPDATED일 때만 의미
) {
    public enum Kind { UPDATED, DISABLED, DELETED }

    public static PostCacheEvent updated(Long postId, boolean detailChanged, boolean imagesChanged) {
        return new PostCacheEvent(postId, Kind.UPDATED, detailChanged, imagesChanged);
    }
    public static PostCacheEvent disabled(Long postId) {
        return new PostCacheEvent(postId, Kind.DISABLED, false, false);
    }
    public static PostCacheEvent deleted(Long postId) {
        return new PostCacheEvent(postId, Kind.DELETED, false, false);
    }
}
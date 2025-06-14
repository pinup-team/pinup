package kr.co.pinup.api.kakao.exception;

public class KakaoApiException extends RuntimeException {

    private static final String DEFAULT_MESSAGE = "잘못된 주소입니다.";

    public KakaoApiException() {
        this(DEFAULT_MESSAGE);
    }

    public KakaoApiException(final String message) {
        super(message);
    }

    public KakaoApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

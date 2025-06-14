package kr.co.pinup.api.aws.exception;

public class SecretsManagerFetchException extends RuntimeException {

    public SecretsManagerFetchException() {
    }

    public SecretsManagerFetchException(final String message) {
        super(message);
    }

    public SecretsManagerFetchException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

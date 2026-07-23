package ru.kirzhq.wazzup.exception;

public class WazzupApiException extends RuntimeException {

    public WazzupApiException(String message) {
        super(message);
    }

    public WazzupApiException(
            String message,
            Throwable cause
    ) {
        super(message, cause);
    }
}
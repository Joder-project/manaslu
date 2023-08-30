package org.manaslu.cache.core.exception;

public class ManasluException extends RuntimeException {
    public ManasluException() {
    }

    public ManasluException(String message) {
        super(message);
    }

    public ManasluException(String message, Throwable cause) {
        super(message, cause);
    }

    public ManasluException(Throwable cause) {
        super(cause);
    }

    public ManasluException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}

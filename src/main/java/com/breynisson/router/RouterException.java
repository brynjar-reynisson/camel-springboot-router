package com.breynisson.router;

public class RouterException extends RuntimeException {

    public RouterException(String message) {
        super(message);
    }

    public RouterException(String message, Throwable cause) {
        super(message, cause);
    }

    public RouterException(Exception e) {
        super(e);
    }
}

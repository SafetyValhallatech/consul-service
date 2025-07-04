package org.devquality.consulservice.exception;

import org.springframework.http.HttpStatus;

public class ConsulConnectionException extends ConsulServiceException {
    public ConsulConnectionException(String message) {
        super(message, HttpStatus.SERVICE_UNAVAILABLE, "CONSUL_CONNECTION_FAILED");
    }

    public ConsulConnectionException(String message, Throwable cause) {
        super(message, cause, HttpStatus.SERVICE_UNAVAILABLE);
    }
}

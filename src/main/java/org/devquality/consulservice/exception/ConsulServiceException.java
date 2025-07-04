package org.devquality.consulservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ConsulServiceException extends RuntimeException {
    private final HttpStatus httpStatus;
    private final String errorCode;

    public ConsulServiceException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = httpStatus.name();
    }

    public ConsulServiceException(String message, HttpStatus httpStatus, String errorCode) {
        super(message);
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
    }

    public ConsulServiceException(String message, Throwable cause, HttpStatus httpStatus) {
        super(message, cause);
        this.httpStatus = httpStatus;
        this.errorCode = httpStatus.name();
    }
}



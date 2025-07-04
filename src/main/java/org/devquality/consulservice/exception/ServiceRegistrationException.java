package org.devquality.consulservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceRegistrationException extends ConsulServiceException {
    public ServiceRegistrationException(String message) {
        super(message, HttpStatus.BAD_REQUEST, "SERVICE_REGISTRATION_FAILED");
    }

    public ServiceRegistrationException(String message, Throwable cause) {
        super(message, cause, HttpStatus.BAD_REQUEST);
    }
}
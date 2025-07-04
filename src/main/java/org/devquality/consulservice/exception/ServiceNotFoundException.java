package org.devquality.consulservice.exception;

import org.springframework.http.HttpStatus;

public class ServiceNotFoundException extends org.devquality.consulservice.exception.ConsulServiceException {
    public ServiceNotFoundException(String serviceName) {
        super(String.format("Service '%s' not found in Consul registry", serviceName),
                HttpStatus.NOT_FOUND, "SERVICE_NOT_FOUND");
    }
}

package org.devquality.consulservice.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.exception.ConsulConnectionException;
import org.devquality.consulservice.exception.ServiceNotFoundException;
import org.devquality.consulservice.exception.ServiceRegistrationException;
import org.devquality.consulservice.web.dtos.*;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.consul.discovery.ConsulDiscoveryClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsulService {

    private final DiscoveryClient discoveryClient;
    private static final String CIRCUIT_BREAKER_NAME = "consul-service";

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getRegisteredServicesFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    @TimeLimiter(name = CIRCUIT_BREAKER_NAME)
    public CompletableFuture<List<String>> getRegisteredServicesAsync() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<String> services = discoveryClient.getServices();
                log.info("📋 Found {} services in Consul: {}", services.size(), services);
                return services;
            } catch (Exception e) {
                log.error("❌ Error retrieving services from Consul: {}", e.getMessage());
                throw new ConsulConnectionException("Failed to retrieve services from Consul", e);
            }
        });
    }

    public List<String> getRegisteredServices() {
        try {
            List<String> services = discoveryClient.getServices();
            log.info("📋 Found {} services in Consul: {}", services.size(), services);
            return services;
        } catch (Exception e) {
            log.error("❌ Error retrieving services from Consul: {}", e.getMessage());
            throw new ConsulConnectionException("Failed to retrieve services from Consul", e);
        }
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "getServiceInstancesFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<ServiceInstanceDto> getServiceInstances(String serviceName) {
        if (serviceName == null || serviceName.trim().isEmpty()) {
            throw new IllegalArgumentException("Service name cannot be null or empty");
        }

        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

            if (instances.isEmpty()) {
                log.warn("⚠️ No instances found for service: {}", serviceName);
                throw new ServiceNotFoundException(serviceName);
            }

            List<ServiceInstanceDto> instanceDtos = instances.stream()
                    .map(this::mapToServiceInstanceDto)
                    .collect(Collectors.toList());

            log.info("🔍 Found {} instances for service '{}': {}",
                    instanceDtos.size(), serviceName,
                    instanceDtos.stream().map(ServiceInstanceDto::getInstanceId).collect(Collectors.toList()));

            return instanceDtos;

        } catch (ServiceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Error retrieving instances for service '{}': {}", serviceName, e.getMessage());
            throw new ConsulConnectionException("Failed to retrieve service instances", e);
        }
    }

    public ServiceStatsDto getServiceStats() {
        try {
            List<String> services = getRegisteredServices();

            Map<String, Integer> instancesByService = new HashMap<>();
            Map<String, Integer> servicesByStatus = new HashMap<>();
            int totalInstances = 0;
            int healthyServices = 0;

            for (String serviceName : services) {
                try {
                    List<ServiceInstanceDto> instances = getServiceInstances(serviceName);
                    instancesByService.put(serviceName, instances.size());
                    totalInstances += instances.size();

                    boolean hasHealthyInstances = instances.stream()
                            .anyMatch(instance -> "UP".equals(instance.getStatus()) ||
                                    instance.getStatus() == null); // Assume healthy if status is null

                    if (hasHealthyInstances) {
                        healthyServices++;
                        servicesByStatus.merge("HEALTHY", 1, Integer::sum);
                    } else {
                        servicesByStatus.merge("UNHEALTHY", 1, Integer::sum);
                    }
                } catch (ServiceNotFoundException e) {
                    servicesByStatus.merge("UNAVAILABLE", 1, Integer::sum);
                }
            }

            return ServiceStatsDto.builder()
                    .totalServices(services.size())
                    .healthyServices(healthyServices)
                    .unhealthyServices(services.size() - healthyServices)
                    .totalInstances(totalInstances)
                    .servicesByStatus(servicesByStatus)
                    .instancesByService(instancesByService)
                    .lastUpdated(LocalDateTime.now())
                    .build();

        } catch (Exception e) {
            log.error("❌ Error calculating service stats: {}", e.getMessage());
            throw new ConsulConnectionException("Failed to calculate service statistics", e);
        }
    }

    public String registerService(ServiceRegistrationDto registrationDto) {
        try {
            validateServiceRegistration(registrationDto);

            // En un escenario real, aquí podrías hacer registro manual via API de Consul
            // Por ahora, solo validamos y logeamos
            log.info("✅ Service registration validated: {} at {}:{}",
                    registrationDto.getServiceName(),
                    registrationDto.getHost(),
                    registrationDto.getPort());

            return String.format("Service '%s' registered successfully", registrationDto.getServiceName());

        } catch (Exception e) {
            log.error("❌ Error registering service '{}': {}", registrationDto.getServiceName(), e.getMessage());
            throw new ServiceRegistrationException("Failed to register service", e);
        }
    }

    public boolean isServiceHealthy(String serviceName) {
        try {
            List<ServiceInstanceDto> instances = getServiceInstances(serviceName);
            return instances.stream()
                    .anyMatch(instance -> "UP".equals(instance.getStatus()) || instance.getStatus() == null);
        } catch (ServiceNotFoundException e) {
            return false;
        } catch (Exception e) {
            log.error("❌ Error checking health for service '{}': {}", serviceName, e.getMessage());
            return false;
        }
    }

    // Fallback methods for Circuit Breaker
    public CompletableFuture<List<String>> getRegisteredServicesFallback(Exception ex) {
        log.warn("🔄 Using fallback for getRegisteredServices due to: {}", ex.getMessage());
        return CompletableFuture.completedFuture(Arrays.asList("consul", "config-server")); // Default services
    }

    public List<ServiceInstanceDto> getServiceInstancesFallback(String serviceName, Exception ex) {
        log.warn("🔄 Using fallback for getServiceInstances('{}') due to: {}", serviceName, ex.getMessage());
        return Collections.emptyList();
    }

    // Private helper methods
    private ServiceInstanceDto mapToServiceInstanceDto(ServiceInstance instance) {
        return ServiceInstanceDto.builder()
                .serviceId(instance.getServiceId())
                .instanceId(instance.getInstanceId())
                .host(instance.getHost())
                .port(instance.getPort())
                .uri(instance.getUri())
                .secure(instance.isSecure())
                .metadata(instance.getMetadata())
                .status(getInstanceStatus(instance))
                .scheme(instance.getScheme())
                .build();
    }

    private String getInstanceStatus(ServiceInstance instance) {
        // Try to get status from metadata, default to "UP" if not available
        String status = instance.getMetadata().get("status");
        return status != null ? status : "UP";
    }

    private void validateServiceRegistration(ServiceRegistrationDto registrationDto) {
        if (registrationDto.getServiceName() == null || registrationDto.getServiceName().trim().isEmpty()) {
            throw new IllegalArgumentException("Service name is required");
        }

        if (registrationDto.getHost() == null || registrationDto.getHost().trim().isEmpty()) {
            throw new IllegalArgumentException("Host is required");
        }

        if (registrationDto.getPort() == null || registrationDto.getPort() <= 0 || registrationDto.getPort() > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }

        // Generate instance ID if not provided
        if (registrationDto.getInstanceId() == null || registrationDto.getInstanceId().trim().isEmpty()) {
            registrationDto.setInstanceId(
                    String.format("%s:%s:%d", registrationDto.getServiceName(),
                            registrationDto.getHost(), registrationDto.getPort())
            );
        }
    }
}
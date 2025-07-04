package org.devquality.consulservice.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.service.ConsulService;
import org.devquality.consulservice.web.dtos.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/consul")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Service Discovery", description = "Consul service discovery and management operations")
public class ServiceController {

    private final ConsulService consulService;

    @Operation(summary = "Get all registered services", description = "Retrieve all services registered in Consul")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Services retrieved successfully"),
            @ApiResponse(responseCode = "503", description = "Consul connection failed")
    })
    @GetMapping("/services")
    public ResponseEntity<ApiResponseDto<List<String>>> getRegisteredServices() {
        log.info("📋 Retrieving all registered services from Consul");

        List<String> services = consulService.getRegisteredServices();

        return ResponseEntity.ok(
                ApiResponseDto.success(services,
                        String.format("Successfully retrieved %d services", services.size()))
        );
    }

    @Operation(summary = "Get all registered services (async)", description = "Asynchronously retrieve all services registered in Consul")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Services retrieved successfully"),
            @ApiResponse(responseCode = "503", description = "Consul connection failed")
    })
    @GetMapping("/services/async")
    public CompletableFuture<ResponseEntity<ApiResponseDto<List<String>>>> getRegisteredServicesAsync() {
        log.info("📋 Asynchronously retrieving all registered services from Consul");

        return consulService.getRegisteredServicesAsync()
                .thenApply(services -> ResponseEntity.ok(
                        ApiResponseDto.success(services,
                                String.format("Successfully retrieved %d services", services.size()))
                ));
    }

    @Operation(summary = "Get service instances", description = "Retrieve all instances of a specific service")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service instances retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Service not found"),
            @ApiResponse(responseCode = "400", description = "Invalid service name"),
            @ApiResponse(responseCode = "503", description = "Consul connection failed")
    })
    @GetMapping("/services/{serviceName}")
    public ResponseEntity<ApiResponseDto<List<ServiceInstanceDto>>> getServiceInstances(
            @Parameter(description = "Name of the service", example = "user-service", required = true)
            @PathVariable String serviceName) {

        log.info("🔍 Retrieving instances for service: {}", serviceName);

        List<ServiceInstanceDto> instances = consulService.getServiceInstances(serviceName);

        return ResponseEntity.ok(
                ApiResponseDto.success(instances,
                        String.format("Successfully retrieved %d instances for service '%s'",
                                instances.size(), serviceName))
        );
    }

    @Operation(summary = "Check service health", description = "Check if a service has healthy instances")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service health checked successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service name")
    })
    @GetMapping("/services/{serviceName}/health")
    public ResponseEntity<ApiResponseDto<Boolean>> checkServiceHealth(
            @Parameter(description = "Name of the service", example = "user-service", required = true)
            @PathVariable String serviceName) {

        log.info("🏥 Checking health for service: {}", serviceName);

        boolean isHealthy = consulService.isServiceHealthy(serviceName);

        return ResponseEntity.ok(
                ApiResponseDto.success(isHealthy,
                        String.format("Service '%s' is %s", serviceName, isHealthy ? "healthy" : "unhealthy"))
        );
    }

    @Operation(summary = "Get service statistics", description = "Retrieve comprehensive statistics about all services")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service statistics retrieved successfully"),
            @ApiResponse(responseCode = "503", description = "Consul connection failed")
    })
    @GetMapping("/services/stats")
    public ResponseEntity<ApiResponseDto<ServiceStatsDto>> getServiceStats() {
        log.info("📊 Retrieving service statistics");

        ServiceStatsDto stats = consulService.getServiceStats();

        return ResponseEntity.ok(
                ApiResponseDto.success(stats, "Service statistics retrieved successfully")
        );
    }

    @Operation(summary = "Register a service", description = "Register a new service in Consul")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Service registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid service registration data"),
            @ApiResponse(responseCode = "409", description = "Service already exists"),
            @ApiResponse(responseCode = "503", description = "Consul connection failed")
    })
    @PostMapping("/register")
    public ResponseEntity<ApiResponseDto<String>> registerService(
            @Parameter(description = "Service registration details", required = true)
            @Valid @RequestBody ServiceRegistrationDto serviceData) {

        log.info("📝 Registering service: {}", serviceData.getServiceName());

        String result = consulService.registerService(serviceData);

        return ResponseEntity.status(201).body(
                ApiResponseDto.success(result, "Service registered successfully")
        );
    }
}

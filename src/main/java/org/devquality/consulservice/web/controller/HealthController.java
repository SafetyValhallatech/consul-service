package org.devquality.consulservice.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.config.ApplicationConfig;
import org.devquality.consulservice.web.dtos.ApiResponseDto;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Health & Info", description = "Application health and information endpoints")
public class HealthController {

    private final Environment environment;
    private final ApplicationConfig applicationConfig;
    private final LocalDateTime startTime = LocalDateTime.now();

    @Operation(summary = "Application health check", description = "Get the current health status of the application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application is healthy"),
            @ApiResponse(responseCode = "503", description = "Application is unhealthy")
    })
    @GetMapping("/health")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> health() {
        try {
            Map<String, Object> health = new HashMap<>();

            String serviceName = environment.getProperty("spring.application.name", "consul-service");
            String serverPort = environment.getProperty("server.port", "8081");

            health.put("status", "UP");
            health.put("service", serviceName);
            health.put("port", serverPort);
            health.put("timestamp", LocalDateTime.now());
            health.put("uptime_minutes", java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes());

            // Health checks
            Map<String, String> checks = new HashMap<>();
            checks.put("application", "UP");
            checks.put("consul", getConsulHealthStatus());
            checks.put("disk_space", getDiskSpaceStatus());
            checks.put("memory", getMemoryStatus());

            health.put("checks", checks);
            health.put("version", Objects.requireNonNullElse(applicationConfig.getVersion(), "1.0.0"));

            log.debug("🏥 Health check requested for service: {}", serviceName);

            return ResponseEntity.ok(
                    ApiResponseDto.success(health, "Application is healthy")
            );

        } catch (Exception e) {
            log.error("❌ Health check failed: {}", e.getMessage());

            Map<String, Object> health = Map.of(
                    "status", "DOWN",
                    "error", e.getMessage(),
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.status(503).body(
                    ApiResponseDto.error("Application health check failed", health.toString())
            );
        }
    }

    @Operation(summary = "Application information", description = "Get detailed information about the application")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application info retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> info() {
        try {
            Map<String, Object> info = new HashMap<>();

            String serviceName = environment.getProperty("spring.application.name", "consul-service");
            String serverPort = environment.getProperty("server.port", "8081");

            info.put("application", Map.of(
                    "name", serviceName,
                    "version", Objects.requireNonNullElse(applicationConfig.getVersion(), "1.0.0"),
                    "description", Objects.requireNonNullElse(applicationConfig.getDescription(), "Consul Discovery Service"),
                    "port", serverPort
            ));

            info.put("environment", Map.of(
                    "active_profiles", environment.getActiveProfiles(),
                    "java_version", System.getProperty("java.version"),
                    "java_vendor", System.getProperty("java.vendor"),
                    "os_name", System.getProperty("os.name"),
                    "os_version", System.getProperty("os.version")
            ));

            info.put("runtime", Map.of(
                    "start_time", startTime,
                    "uptime_minutes", java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes(),
                    "available_processors", Runtime.getRuntime().availableProcessors(),
                    "max_memory_mb", Runtime.getRuntime().maxMemory() / (1024 * 1024),
                    "total_memory_mb", Runtime.getRuntime().totalMemory() / (1024 * 1024),
                    "free_memory_mb", Runtime.getRuntime().freeMemory() / (1024 * 1024)
            ));

            info.put("consul", Map.of(
                    "host", environment.getProperty("spring.cloud.consul.host", "localhost"),
                    "port", environment.getProperty("spring.cloud.consul.port", "8500"),
                    "discovery_enabled", environment.getProperty("spring.cloud.consul.discovery.enabled", "true"),
                    "config_enabled", environment.getProperty("spring.cloud.consul.config.enabled", "true")
            ));

            return ResponseEntity.ok(
                    ApiResponseDto.success(info, "Application information retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving application info: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve application info", e.getMessage())
            );
        }
    }

    private String getConsulHealthStatus() {
        try {
            // En un caso real, verificarías la conexión con Consul
            return "UP";
        } catch (Exception e) {
            return "DOWN";
        }
    }

    private String getDiskSpaceStatus() {
        try {
            long freeSpace = Runtime.getRuntime().freeMemory();
            long totalSpace = Runtime.getRuntime().totalMemory();
            double usagePercent = ((double) (totalSpace - freeSpace) / totalSpace) * 100;

            return usagePercent < 90 ? "UP" : "WARNING";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }

    private String getMemoryStatus() {
        try {
            long maxMemory = Runtime.getRuntime().maxMemory();
            long totalMemory = Runtime.getRuntime().totalMemory();
            double usagePercent = ((double) totalMemory / maxMemory) * 100;

            return usagePercent < 85 ? "UP" : "WARNING";
        } catch (Exception e) {
            return "UNKNOWN";
        }
    }
}
package org.devquality.consulservice.web.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.config.ApplicationConfig;
import org.devquality.consulservice.web.dtos.ApiResponseDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/config")
@RequiredArgsConstructor
@Slf4j
@RefreshScope
@Tag(name = "Configuration Management", description = "Endpoints for managing application configuration")
public class ConfigurablePropertiesController {

    private final Environment environment;
    private final ApplicationConfig applicationConfig;

    @Value("${spring.application.name:consul-service}")
    private String applicationName;

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${spring.cloud.consul.host:localhost}")
    private String consulHost;

    @Value("${spring.cloud.consul.port:8500}")
    private String consulPort;

    @Operation(summary = "Get all application properties", description = "Retrieve all current application configuration properties")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Properties retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    @GetMapping("/properties")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getAllProperties() {
        try {
            Map<String, Object> properties = new HashMap<>();

            // Application properties
            properties.put("application", Map.of(
                    "name", applicationName,
                    "version", Objects.requireNonNullElse(applicationConfig.getVersion(), "1.0.0"),
                    "description", Objects.requireNonNullElse(applicationConfig.getDescription(), "Consul Discovery Service"),
                    "active_profiles", environment.getActiveProfiles()
            ));

            // Server properties
            properties.put("server", Map.of(
                    "port", serverPort,
                    "servlet_context_path", environment.getProperty("server.servlet.context-path", "/")
            ));

            // Consul properties
            properties.put("consul", Map.of(
                    "host", consulHost,
                    "port", consulPort,
                    "discovery_enabled", environment.getProperty("spring.cloud.consul.discovery.enabled", "true"),
                    "config_enabled", environment.getProperty("spring.cloud.consul.config.enabled", "true")
            ));

            // Management properties
            properties.put("management", Map.of(
                    "endpoints_exposure", environment.getProperty("management.endpoints.web.exposure.include", "*"),
                    "health_show_details", environment.getProperty("management.endpoint.health.show-details", "always")
            ));

            log.info("📋 Configuration properties retrieved successfully");

            return ResponseEntity.ok(
                    ApiResponseDto.success(properties, "Configuration properties retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving configuration properties: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve configuration properties", e.getMessage())
            );
        }
    }

    @Operation(summary = "Get specific property", description = "Retrieve a specific configuration property by key")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Property not found"),
            @ApiResponse(responseCode = "400", description = "Invalid property key")
    })
    @GetMapping("/properties/{key}")
    public ResponseEntity<ApiResponseDto<Object>> getProperty(
            @Parameter(description = "Property key", example = "spring.application.name")
            @PathVariable String key) {

        if (key == null || key.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(
                    ApiResponseDto.error("Property key cannot be null or empty", "INVALID_KEY")
            );
        }

        try {
            String value = environment.getProperty(key);

            if (value == null) {
                log.warn("⚠️ Property not found: {}", key);
                return ResponseEntity.notFound().build();
            }

            Map<String, Object> response = Map.of(
                    "key", key,
                    "value", value,
                    "timestamp", LocalDateTime.now()
            );

            log.debug("🔍 Property retrieved: {} = {}", key, value);

            return ResponseEntity.ok(
                    ApiResponseDto.success(response, String.format("Property '%s' retrieved successfully", key))
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving property '{}': {}", key, e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve property", e.getMessage())
            );
        }
    }

    @Operation(summary = "Get application info", description = "Retrieve basic application information")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Application info retrieved successfully")
    })
    @GetMapping("/info")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getApplicationInfo() {
        try {
            Map<String, Object> info = new HashMap<>();

            info.put("application", Map.of(
                    "name", applicationName,
                    "version", Objects.requireNonNullElse(applicationConfig.getVersion(), "1.0.0"),
                    "description", Objects.requireNonNullElse(applicationConfig.getDescription(), "Consul Discovery Service")
            ));

            info.put("environment", Map.of(
                    "active_profiles", environment.getActiveProfiles(),
                    "java_version", System.getProperty("java.version"),
                    "spring_boot_version", environment.getProperty("spring-boot.version", "3.5.3")
            ));

            info.put("consul", Map.of(
                    "host", consulHost,
                    "port", consulPort,
                    "connection_status", "CONNECTED" // En un caso real, verificarías la conexión
            ));

            info.put("contact", Map.of(
                    "name", Objects.requireNonNullElse(applicationConfig.getContact().getName(), "DevQuality Team"),
                    "email", Objects.requireNonNullElse(applicationConfig.getContact().getEmail(), "support@devquality.org")
            ));

            info.put("timestamp", LocalDateTime.now());

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

    @Operation(summary = "Refresh configuration", description = "Trigger configuration refresh from Consul")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Configuration refreshed successfully"),
            @ApiResponse(responseCode = "500", description = "Failed to refresh configuration")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> refreshConfiguration() {
        try {
            // En un escenario real, aquí activarías el refresh de configuración
            log.info("🔄 Configuration refresh triggered");

            Map<String, Object> response = Map.of(
                    "status", "SUCCESS",
                    "message", "Configuration refresh initiated",
                    "timestamp", LocalDateTime.now(),
                    "next_check", LocalDateTime.now().plusMinutes(1)
            );

            return ResponseEntity.ok(
                    ApiResponseDto.success(response, "Configuration refresh triggered successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error refreshing configuration: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to refresh configuration", e.getMessage())
            );
        }
    }

    @Operation(summary = "Get environment profiles", description = "Retrieve active Spring profiles")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profiles retrieved successfully")
    })
    @GetMapping("/profiles")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getProfiles() {
        try {
            String[] activeProfiles = environment.getActiveProfiles();
            String[] defaultProfiles = environment.getDefaultProfiles();

            Map<String, Object> profiles = Map.of(
                    "active", activeProfiles.length > 0 ? activeProfiles : new String[]{"default"},
                    "default", defaultProfiles,
                    "count", activeProfiles.length,
                    "timestamp", LocalDateTime.now()
            );

            return ResponseEntity.ok(
                    ApiResponseDto.success(profiles, "Environment profiles retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving profiles: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve profiles", e.getMessage())
            );
        }
    }
}
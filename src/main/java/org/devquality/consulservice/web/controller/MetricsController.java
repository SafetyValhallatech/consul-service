package org.devquality.consulservice.web.controller;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.service.ConsulService;
import org.devquality.consulservice.web.dtos.ApiResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Metrics & Monitoring", description = "Application and system metrics endpoints")
public class MetricsController {

    private final ConsulService consulService;
    private final MeterRegistry meterRegistry;
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    private final LocalDateTime startTime = LocalDateTime.now();

    @Operation(summary = "Get custom application metrics", description = "Retrieve detailed application and system metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Metrics retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving metrics")
    })
    @GetMapping("/custom")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getCustomMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // Memory metrics
            long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
            long heapMax = memoryBean.getHeapMemoryUsage().getMax();
            long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();
            long nonHeapMax = memoryBean.getNonHeapMemoryUsage().getMax();

            metrics.put("memory", Map.of(
                    "heap_used_mb", heapUsed / (1024 * 1024),
                    "heap_max_mb", heapMax / (1024 * 1024),
                    "heap_usage_percent", Math.round((double) heapUsed / heapMax * 100),
                    "non_heap_used_mb", nonHeapUsed / (1024 * 1024),
                    "non_heap_max_mb", nonHeapMax > 0 ? nonHeapMax / (1024 * 1024) : -1,
                    "total_memory_mb", Runtime.getRuntime().totalMemory() / (1024 * 1024),
                    "free_memory_mb", Runtime.getRuntime().freeMemory() / (1024 * 1024)
            ));

            // System metrics
            metrics.put("system", Map.of(
                    "available_processors", osBean.getAvailableProcessors(),
                    "system_load_average", osBean.getSystemLoadAverage(),
                    "arch", osBean.getArch(),
                    "os_name", osBean.getName(),
                    "os_version", osBean.getVersion()
            ));

            // Runtime metrics
            metrics.put("runtime", Map.of(
                    "uptime_ms", runtimeBean.getUptime(),
                    "uptime_minutes", runtimeBean.getUptime() / (1000 * 60),
                    "start_time", LocalDateTime.now().minusNanos(runtimeBean.getUptime() * 1_000_000),
                    "jvm_name", runtimeBean.getVmName(),
                    "jvm_version", runtimeBean.getVmVersion(),
                    "jvm_vendor", runtimeBean.getVmVendor()
            ));

            // Application metrics
            metrics.put("application", Map.of(
                    "service_start_time", startTime,
                    "current_time", LocalDateTime.now(),
                    "service_uptime_minutes", java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes()
            ));

            // Consul metrics (if available)
            try {
                var serviceStats = consulService.getServiceStats();
                metrics.put("consul", Map.of(
                        "total_services", serviceStats.getTotalServices(),
                        "healthy_services", serviceStats.getHealthyServices(),
                        "unhealthy_services", serviceStats.getUnhealthyServices(),
                        "total_instances", serviceStats.getTotalInstances(),
                        "last_updated", serviceStats.getLastUpdated()
                ));
            } catch (Exception e) {
                log.warn("⚠️ Could not retrieve Consul metrics: {}", e.getMessage());
                metrics.put("consul", Map.of("status", "UNAVAILABLE", "error", e.getMessage()));
            }

            log.debug("📊 Custom metrics retrieved successfully");

            return ResponseEntity.ok(
                    ApiResponseDto.success(metrics, "Custom metrics retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving custom metrics: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve custom metrics", e.getMessage())
            );
        }
    }

    @Operation(summary = "Get service status summary", description = "Get a summary of the current service status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Service status retrieved successfully"),
            @ApiResponse(responseCode = "500", description = "Error retrieving service status")
    })
    @GetMapping("/status")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getServiceStatus() {
        try {
            Map<String, Object> status = new HashMap<>();

            // Overall health status
            boolean memoryOk = (memoryBean.getHeapMemoryUsage().getUsed() * 100 /
                    memoryBean.getHeapMemoryUsage().getMax()) < 90;

            boolean loadOk = osBean.getSystemLoadAverage() < osBean.getAvailableProcessors() * 0.8;

            String overallStatus = (memoryOk && loadOk) ? "HEALTHY" : "WARNING";

            status.put("status", overallStatus);
            status.put("timestamp", LocalDateTime.now());
            status.put("service", "consul-service");

            // Individual checks
            Map<String, String> checks = new HashMap<>();
            checks.put("memory", memoryOk ? "OK" : "WARNING");
            checks.put("cpu_load", loadOk ? "OK" : "WARNING");

            try {
                consulService.getRegisteredServices();
                checks.put("consul_connection", "OK");
            } catch (Exception e) {
                checks.put("consul_connection", "ERROR");
                overallStatus = "UNHEALTHY";
            }

            checks.put("config", "LOADED");
            status.put("checks", checks);
            status.put("overall_status", overallStatus);

            // Performance indicators
            status.put("performance", Map.of(
                    "memory_usage_percent", Math.round((double) memoryBean.getHeapMemoryUsage().getUsed() /
                            memoryBean.getHeapMemoryUsage().getMax() * 100),
                    "cpu_load_average", osBean.getSystemLoadAverage(),
                    "uptime_hours", runtimeBean.getUptime() / (1000 * 60 * 60)
            ));

            return ResponseEntity.ok(
                    ApiResponseDto.success(status, "Service status retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving service status: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve service status", e.getMessage())
            );
        }
    }

    @Operation(summary = "Get Micrometer metrics summary", description = "Get a summary of available Micrometer metrics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Micrometer metrics summary retrieved successfully")
    })
    @GetMapping("/micrometer")
    public ResponseEntity<ApiResponseDto<Map<String, Object>>> getMicrometerMetrics() {
        try {
            Map<String, Object> metrics = new HashMap<>();

            // Get some key metrics from Micrometer
            var httpRequestsTotal = meterRegistry.get("http.server.requests").timer();
            var jvmMemoryUsed = meterRegistry.get("jvm.memory.used").gauge();
            var systemCpuUsage = meterRegistry.get("system.cpu.usage").gauge();

            metrics.put("http_requests", Map.of(
                    "total_count", httpRequestsTotal.count(),
                    "total_time_seconds", httpRequestsTotal.totalTime(java.util.concurrent.TimeUnit.SECONDS),
                    "mean_duration_ms", httpRequestsTotal.mean(java.util.concurrent.TimeUnit.MILLISECONDS)
            ));

            metrics.put("jvm_memory_used_bytes", jvmMemoryUsed.value());
            metrics.put("system_cpu_usage", systemCpuUsage.value());

            metrics.put("timestamp", LocalDateTime.now());
            metrics.put("available_meters", meterRegistry.getMeters().size());

            return ResponseEntity.ok(
                    ApiResponseDto.success(metrics, "Micrometer metrics summary retrieved successfully")
            );

        } catch (Exception e) {
            log.error("❌ Error retrieving Micrometer metrics: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    ApiResponseDto.error("Failed to retrieve Micrometer metrics", e.getMessage())
            );
        }
    }
}
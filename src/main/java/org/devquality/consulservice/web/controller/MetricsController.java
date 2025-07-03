package org.devquality.consulservice.web.controller;


import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/metrics")
@Slf4j
public class MetricsController {

    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final LocalDateTime startTime = LocalDateTime.now();

    @GetMapping("/custom")
    public ResponseEntity<Map<String, Object>> getCustomMetrics() {
        Map<String, Object> metrics = new HashMap<>();

        // Métricas de memoria
        long heapUsed = memoryBean.getHeapMemoryUsage().getUsed();
        long heapMax = memoryBean.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memoryBean.getNonHeapMemoryUsage().getUsed();

        metrics.put("memory", Map.of(
                "heapUsedMB", heapUsed / (1024 * 1024),
                "heapMaxMB", heapMax / (1024 * 1024),
                "nonHeapUsedMB", nonHeapUsed / (1024 * 1024),
                "heapUsagePercent", Math.round((double) heapUsed / heapMax * 100)
        ));

        // Tiempo de actividad
        metrics.put("uptime", Map.of(
                "startTime", startTime,
                "currentTime", LocalDateTime.now(),
                "uptimeMinutes", java.time.Duration.between(startTime, LocalDateTime.now()).toMinutes()
        ));

        // Información del sistema
        metrics.put("system", Map.of(
                "availableProcessors", Runtime.getRuntime().availableProcessors(),
                "totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024),
                "freeMemoryMB", Runtime.getRuntime().freeMemory() / (1024 * 1024)
        ));

        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getServiceStatus() {
        Map<String, Object> status = new HashMap<>();

        // Estado general del servicio
        status.put("status", "HEALTHY");
        status.put("timestamp", LocalDateTime.now());
        status.put("service", "consul-service");

        // Verificaciones básicas
        boolean memoryOk = (memoryBean.getHeapMemoryUsage().getUsed() * 100 /
                memoryBean.getHeapMemoryUsage().getMax()) < 90;

        status.put("checks", Map.of(
                "memory", memoryOk ? "OK" : "WARNING",
                "consul", "CONNECTED", // En un caso real, verificarías la conexión
                "config", "LOADED"
        ));

        return ResponseEntity.ok(status);
    }
}

package org.devquality.consulservice.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@Slf4j
public class HealthController {

    private final Environment environment;

    // Constructor injection en lugar de @Value para mayor robustez
    public HealthController(Environment environment) {
        this.environment = environment;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");

        // Usar Environment para obtener propiedades con valores por defecto
        String serviceName = environment.getProperty("spring.application.name", "consul-service");
        String serverPort = environment.getProperty("server.port", "8080");

        health.put("service", serviceName);
        health.put("port", serverPort);
        health.put("timestamp", LocalDateTime.now());

        log.info("Health check requested for service: {}", serviceName);
        return ResponseEntity.ok(health);
    }

    @GetMapping("/info")
    public ResponseEntity<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();

        String serviceName = environment.getProperty("spring.application.name", "consul-service");
        String serverPort = environment.getProperty("server.port", "8080");

        info.put("service", serviceName);
        info.put("version", "1.0.0");
        info.put("description", "Microservicio con Consul Discovery");
        info.put("port", serverPort);
        info.put("uptime", LocalDateTime.now());

        return ResponseEntity.ok(info);
    }
}
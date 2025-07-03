package org.devquality.consulservice.web.controller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.devquality.consulservice.service.ConsulService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/consul")
@RequiredArgsConstructor
@Slf4j
public class ServiceController {

    private final ConsulService consulService;

    @GetMapping("/services")
    public ResponseEntity<List<String>> getRegisteredServices() {
        log.info("Obteniendo servicios registrados en Consul");
        List<String> services = consulService.getRegisteredServices();
        return ResponseEntity.ok(services);
    }

    @GetMapping("/services/{serviceName}")
    public ResponseEntity<List<Map<String, Object>>> getServiceInstances(@PathVariable String serviceName) {
        log.info("Obteniendo instancias del servicio: {}", serviceName);
        List<Map<String, Object>> instances = consulService.getServiceInstances(serviceName);
        return ResponseEntity.ok(instances);
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerService(@RequestBody Map<String, Object> serviceData) {
        log.info("Registrando servicio: {}", serviceData);
        String result = consulService.registerService(serviceData);
        return ResponseEntity.ok(result);
    }
}
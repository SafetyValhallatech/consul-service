package org.devquality.consulservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsulService {

    private final DiscoveryClient discoveryClient;

    public List<String> getRegisteredServices() {
        try {
            List<String> services = discoveryClient.getServices();
            log.info("Servicios encontrados en Consul: {}", services);
            return services;
        } catch (Exception e) {
            log.error("Error al obtener servicios de Consul: {}", e.getMessage());
            throw new RuntimeException("Error al conectar con Consul", e);
        }
    }

    public List<Map<String, Object>> getServiceInstances(String serviceName) {
        try {
            List<ServiceInstance> instances = discoveryClient.getInstances(serviceName);

            return instances.stream().map(instance -> {
                Map<String, Object> instanceInfo = new HashMap<>();
                instanceInfo.put("serviceId", instance.getServiceId());
                instanceInfo.put("host", instance.getHost());
                instanceInfo.put("port", instance.getPort());
                instanceInfo.put("uri", instance.getUri());
                instanceInfo.put("secure", instance.isSecure());
                instanceInfo.put("metadata", instance.getMetadata());
                return instanceInfo;
            }).collect(Collectors.toList());

        } catch (Exception e) {
            log.error("Error al obtener instancias del servicio {}: {}", serviceName, e.getMessage());
            throw new RuntimeException("Error al obtener instancias del servicio", e);
        }
    }

    public String registerService(Map<String, Object> serviceData) {
        // En un caso real, aquí harías el registro manual si fuera necesario
        // Pero Spring Cloud Consul ya se encarga del auto-registro
        log.info("Servicio registrado automáticamente por Spring Cloud Consul");
        return "Servicio registrado exitosamente";
    }
}
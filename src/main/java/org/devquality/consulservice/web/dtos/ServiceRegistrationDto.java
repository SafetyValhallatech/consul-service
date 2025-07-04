package org.devquality.consulservice.web.dtos;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRegistrationDto {

    @NotBlank(message = "Service name is required")
    @Pattern(regexp = "^[a-zA-Z0-9-_]+$", message = "Service name can only contain letters, numbers, hyphens and underscores")
    private String serviceName;

    @NotBlank(message = "Host is required")
    private String host;

    @NotNull(message = "Port is required")
    @Positive(message = "Port must be positive")
    private Integer port;

    private String instanceId;
    private Boolean secure = false;
    private List<String> tags;
    private Map<String, String> metadata;
    private String healthCheckPath = "/actuator/health";
    private Integer healthCheckInterval = 15; // seconds
    private String scheme = "http";
}

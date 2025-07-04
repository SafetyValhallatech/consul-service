package org.devquality.consulservice.web.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceHealthDto {
    private String serviceId;
    private String instanceId;
    private String status; // PASSING, WARNING, CRITICAL
    private String output;
    private LocalDateTime timestamp;
    private Map<String, Object> details;
}

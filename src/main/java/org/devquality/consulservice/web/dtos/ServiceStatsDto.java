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
public class ServiceStatsDto {
    private Integer totalServices;
    private Integer healthyServices;
    private Integer unhealthyServices;
    private Integer totalInstances;
    private Map<String, Integer> servicesByStatus;
    private Map<String, Integer> instancesByService;
    private LocalDateTime lastUpdated;
}

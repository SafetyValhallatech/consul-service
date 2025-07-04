package org.devquality.consulservice.web.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceInstanceDto {
    private String serviceId;
    private String instanceId;
    private String host;
    private Integer port;
    private URI uri;
    private Boolean secure;
    private Map<String, String> metadata;
    private String status;
    private String scheme;
}

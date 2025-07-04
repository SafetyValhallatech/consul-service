package org.devquality.consulservice.web.dtos;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConsulConfigDto {
    private String key;
    private String value;
    private Long createIndex;
    private Long modifyIndex;
    private Long lockIndex;
    private String flags;
    private String session;
    private Map<String, Object> metadata;
}

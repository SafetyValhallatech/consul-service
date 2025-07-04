package org.devquality.consulservice.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Data
public class ApplicationConfig {

    private String name;
    private String version;
    private String description;
    private Contact contact = new Contact();
    private Cors cors = new Cors();

    @Data
    public static class Contact {
        private String name;
        private String email;
    }

    @Data
    public static class Cors {
        private String allowedOrigins;
        private String allowedMethods;
        private String allowedHeaders;
        private String exposedHeaders;
        private boolean allowCredentials = true;
        private long maxAge = 3600;
    }
}
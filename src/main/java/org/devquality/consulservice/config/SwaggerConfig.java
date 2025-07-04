package org.devquality.consulservice.config;

import org.springframework.context.annotation.Configuration;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Consul Discovery Service API",
                description = "Microservice for managing Consul service discovery and configuration",
                version = "1.0.0",
                contact = @Contact(
                        name = "DevQuality Team",
                        email = "support@devquality.org",
                        url = "https://devquality.org"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Development Server",
                        url = "http://localhost:8081"
                ),
                @Server(
                        description = "Production Server",
                        url = "https://consul.devquality.org"
                )
        }
)
public class SwaggerConfig {
}

package org.devquality.consulservice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.time.LocalDateTime;

@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties
@Slf4j
public class ConsulServiceApplication implements CommandLineRunner {

    private final Environment environment;

    public ConsulServiceApplication(Environment environment) {
        this.environment = environment;
    }

    public static void main(String[] args) {
        System.setProperty("spring.application.name", "consul-service");

        // Configurar timezone por defecto
        System.setProperty("user.timezone", "UTC");

        // Configurar encoding
        System.setProperty("file.encoding", "UTF-8");

        try {
            SpringApplication app = new SpringApplication(ConsulServiceApplication.class);

            // Agregar listeners para eventos de aplicación
            app.addListeners(new ApplicationStartupListener());

            var context = app.run(args);

            logApplicationStartup(context.getEnvironment());

        } catch (Exception e) {
            log.error("❌ Failed to start Consul Service application", e);
            System.exit(1);
        }
    }

    @Override
    public void run(String... args) throws Exception {
        // Validaciones post-startup
        validateConfiguration();

        // Log de información adicional
        logRuntimeInformation();

        log.info("🚀 Consul Service application started successfully!");
        log.info("📋 Available endpoints:");
        log.info("   • Swagger UI: http://localhost:{}/swagger-ui.html", getServerPort());
        log.info("   • Health Check: http://localhost:{}/actuator/health", getServerPort());
        log.info("   • Consul Services: http://localhost:{}/api/v1/consul/services", getServerPort());
        log.info("   • Configuration: http://localhost:{}/api/v1/config/properties", getServerPort());
    }

    private static void logApplicationStartup(Environment env) {
        try {
            String serverPort = env.getProperty("server.port", "8081");
            String contextPath = env.getProperty("server.servlet.context-path", "/");
            String hostAddress = InetAddress.getLocalHost().getHostAddress();
            String profiles = env.getActiveProfiles().length == 0 ?
                    env.getDefaultProfiles()[0] : String.join(", ", env.getActiveProfiles());

            log.info("""
                
                ----------------------------------------------------------
                🎉 Application '{}' is running! Access URLs:
                ----------------------------------------------------------
                    Local:      http://localhost:{}{}
                    External:   http://{}:{}{}
                    Profile(s): {}
                    Consul:     http://{}:{}
                ----------------------------------------------------------
                """,
                    env.getProperty("spring.application.name"),
                    serverPort,
                    contextPath,
                    hostAddress,
                    serverPort,
                    contextPath,
                    profiles,
                    env.getProperty("spring.cloud.consul.host", "localhost"),
                    env.getProperty("spring.cloud.consul.port", "8500")
            );

        } catch (Exception e) {
            log.warn("⚠️ Could not determine application URLs: {}", e.getMessage());
        }
    }

    private void validateConfiguration() {
        log.info("🔍 Validating application configuration...");

        // Validar configuración de Consul
        String consulHost = environment.getProperty("spring.cloud.consul.host");
        String consulPort = environment.getProperty("spring.cloud.consul.port");

        if (consulHost == null || consulHost.trim().isEmpty()) {
            log.warn("⚠️ Consul host not configured, using default: localhost");
        }

        if (consulPort == null) {
            log.warn("⚠️ Consul port not configured, using default: 8500");
        }

        // Validar perfiles activos
        String[] activeProfiles = environment.getActiveProfiles();
        if (activeProfiles.length == 0) {
            log.warn("⚠️ No active profiles set, using default profile");
        } else {
            log.info("✅ Active profiles: {}", String.join(", ", activeProfiles));
        }

        // Validar propiedades críticas
        String appName = environment.getProperty("spring.application.name");
        if (appName == null || appName.trim().isEmpty()) {
            log.error("❌ Application name not configured!");
            throw new IllegalStateException("spring.application.name must be configured");
        }

        log.info("✅ Configuration validation completed successfully");
    }

    private void logRuntimeInformation() {
        Runtime runtime = Runtime.getRuntime();

        log.info("💻 Runtime Information:");
        log.info("   • Java Version: {}", System.getProperty("java.version"));
        log.info("   • Java Vendor: {}", System.getProperty("java.vendor"));
        log.info("   • OS: {} {} {}",
                System.getProperty("os.name"),
                System.getProperty("os.version"),
                System.getProperty("os.arch"));
        log.info("   • Available Processors: {}", runtime.availableProcessors());
        log.info("   • Max Memory: {} MB", runtime.maxMemory() / 1024 / 1024);
        log.info("   • Total Memory: {} MB", runtime.totalMemory() / 1024 / 1024);
        log.info("   • Free Memory: {} MB", runtime.freeMemory() / 1024 / 1024);
        log.info("   • Startup Time: {}", LocalDateTime.now());
    }

    private String getServerPort() {
        return environment.getProperty("server.port", "8081");
    }

    // Listener para eventos de startup
    private static class ApplicationStartupListener implements org.springframework.context.ApplicationListener<org.springframework.boot.context.event.ApplicationReadyEvent> {
        @Override
        public void onApplicationEvent(org.springframework.boot.context.event.ApplicationReadyEvent event) {
            log.info("🟢 Application startup completed successfully");
        }
    }
}
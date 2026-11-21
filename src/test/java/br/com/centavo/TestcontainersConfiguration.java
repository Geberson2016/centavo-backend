package br.com.centavo;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

// Postgres descartável para os testes de repository e de integração. O @ServiceConnection
// sobrepõe o spring.datasource.*: os testes nunca tocam o banco de dev, nem em máquina com
// DB_URL no ambiente. Contextos iguais são reaproveitados pelo Spring, e o container junto.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    // Mesma imagem do docker-compose de dev.
    private static final DockerImageName POSTGRES = DockerImageName.parse("postgres:16-alpine");

    @Bean
    @ServiceConnection
    PostgreSQLContainer<?> postgresContainer() {
        return new PostgreSQLContainer<>(POSTGRES);
    }
}

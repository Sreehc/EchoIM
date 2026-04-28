package com.echoim.server;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class EchoImServerApplicationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.4")
            .withDatabaseName("echoim")
            .withUsername("root")
            .withPassword("root");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7.4").withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("MYSQL_HOST", MYSQL::getHost);
        registry.add("MYSQL_PORT", () -> MYSQL.getMappedPort(3306));
        registry.add("MYSQL_DB", MYSQL::getDatabaseName);
        registry.add("MYSQL_USERNAME", MYSQL::getUsername);
        registry.add("MYSQL_PASSWORD", MYSQL::getPassword);
        registry.add("REDIS_HOST", REDIS::getHost);
        registry.add("REDIS_PORT", () -> REDIS.getMappedPort(6379));
        registry.add("ECHOIM_JWT_SECRET", () -> "test-jwt-secret-key-1234567890123456");
        registry.add("ECHOIM_FILE_STORAGE_TYPE", () -> "oss");
        registry.add("ECHOIM_IM_PORT", () -> "18091");
        registry.add("echoim.file.oss.endpoint", () -> "https://oss-cn-beijing.aliyuncs.com");
        registry.add("echoim.file.oss.bucket-name", () -> "test-bucket");
        registry.add("echoim.file.oss.access-key-id", () -> "test-key-id");
        registry.add("echoim.file.oss.access-key-secret", () -> "test-key-secret");
    }

    @Test
    void healthEndpointShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }
}

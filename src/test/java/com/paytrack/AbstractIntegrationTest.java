package com.paytrack;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRE_SQL_CONTAINER = new PostgreSQLContainer<>("postgres:15-alpine");
	@ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            POSTGRE_SQL_CONTAINER
                    .withDatabaseName("paytrack")
                    .withUsername("paytrack")
                    .withPassword("paytrack");

    static {
        POSTGRES.start();
    }
}
package org.example.productcatalogapi;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "pricing.enabled=false")
@Testcontainers
class ProductCatalogApiApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    @Test
    void shouldLoadCatalogApplication(ApplicationContext context) {
        assertThat(context.getBean(ProductCatalogApiApplication.class)).isNotNull();
    }

    @Test
    void shouldNotReapplyMigrationsOnSubsequentRuns(@Autowired Flyway flyway) {
        assertThat(flyway.migrate().migrationsExecuted).isZero();
    }

}

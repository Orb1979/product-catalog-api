package org.example.productcatalogapi.migration;

import java.time.Instant;
import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class CatalogMigrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final JdbcTemplate jdbc;

    @Autowired
    CatalogMigrationTest(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Test
    void shouldInitializePricingCursorAtEpoch() {
        OffsetDateTime cursor = jdbc.queryForObject(
                "SELECT last_synced_at FROM sync_state WHERE name = 'pricing'", OffsetDateTime.class);

        assertThat(cursor).isNotNull();
        assertThat(cursor.toInstant()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void shouldCreateGinSearchIndex() {
        String definition = jdbc.queryForObject("""
                SELECT indexdef FROM pg_indexes
                WHERE schemaname = 'public' AND indexname = 'idx_products_search'
                """, String.class);

        assertThat(definition).contains("USING gin (search_vector)");
    }

}

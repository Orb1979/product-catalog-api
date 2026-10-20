package org.example.productcatalogapi.search;

import java.util.Map;

import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
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
class ProductSearchTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final ProductRepository repository;
    private final JdbcTemplate jdbc;

    @Autowired
    ProductSearchTest(ProductRepository repository, JdbcTemplate jdbc) {
        this.repository = repository;
        this.jdbc = jdbc;
    }

    @Test
    void shouldGenerateSearchVectorForJsonAttributesOnInsertAndUpdate() {
        repository.saveAndFlush(new Product("TEST-SEARCH", "Example", null, null,
                null, Map.of("material", "titanium")));

        assertThat(matchesSearch("TEST-SEARCH", "titanium")).isTrue();

        jdbc.update("UPDATE products SET attributes = ?::jsonb WHERE sku = ?",
                "{\"material\":\"aluminium\"}", "TEST-SEARCH");

        assertThat(matchesSearch("TEST-SEARCH", "aluminium")).isTrue();
        assertThat(matchesSearch("TEST-SEARCH", "titanium")).isFalse();
    }

    private boolean matchesSearch(String sku, String term) {
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT search_vector @@ plainto_tsquery('simple', ?)
                FROM products WHERE sku = ?
                """, Boolean.class, term, sku));
    }

}

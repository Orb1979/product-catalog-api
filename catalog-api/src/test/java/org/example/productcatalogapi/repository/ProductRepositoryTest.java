package org.example.productcatalogapi.repository;

import java.util.Map;

import jakarta.persistence.EntityManager;
import org.example.productcatalogapi.model.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Testcontainers
class ProductRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final ProductRepository repository;
    private final EntityManager entityManager;

    @Autowired
    ProductRepositoryTest(ProductRepository repository, EntityManager entityManager) {
        this.repository = repository;
        this.entityManager = entityManager;
    }

    @Test
    void shouldSeedTenThousandProductsWithMatchingSkuRange() {
        assertThat(repository.count()).isEqualTo(10_000);
    }

    @Test
    void shouldLoadSeededCharacteristicsAndUnpricedState() {
        Product product = repository.findById("SKU-000001").orElseThrow();

        assertThat(product.getName()).isEqualTo("City Lamp 1");
        assertThat(product.getDescription()).isEqualTo("Description for product 1. Durable and well designed.");
        assertThat(product.getBrand()).isEqualTo("Nordic");
        assertThat(product.getCategory()).isEqualTo("Home");
        assertThat(product.getAttributes()).containsExactlyInAnyOrderEntriesOf(Map.of("color", "blue", "size", "M"));
        assertThat(product.getPrice()).isNull();
        assertThat(product.getCurrency()).isNull();
        assertThat(product.getPriceUpdatedAt()).isNull();
    }

    @Test
    void shouldPersistNestedJsonAttributes() {
        Map<String, Object> attributes = Map.of(
            "color", "purple",
            "specifications", Map.of("wireless", true, "weightGrams", 250));
        Product product = new Product("TEST-JSON", "Test headphones", null, null, "Audio", attributes);

        repository.saveAndFlush(product);
        entityManager.clear();

        Product loaded = repository.findById("TEST-JSON").orElseThrow();

        assertThat(loaded.getAttributes()).containsExactlyInAnyOrderEntriesOf(attributes);
    }
}

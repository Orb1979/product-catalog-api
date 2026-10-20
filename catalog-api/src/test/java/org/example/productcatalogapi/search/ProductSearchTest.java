package org.example.productcatalogapi.search;

import java.util.List;
import java.util.Map;

import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

    @ParameterizedTest
    @ValueSource(strings = {"CAMPING", "drinks", "Decathlon", "small", "large", "stainless"})
    void shouldSearchAcrossDescriptiveFieldsAndJsonAttributes(String query) {
        seedSearchProduct();

        Page<Product> results = repository.search(query, PageRequest.of(0, 20));

        assertThat(results.getContent()).extracting(Product::getSku).containsExactly("TEST-SEARCH");
    }

    @ParameterizedTest
    @CsvSource({
            "camping large, 1",
            "'\"camping coffee\"', 1",
            "camping OR nonexistent, 1",
            "camping -large, 0",
            "nonexistent, 0",
            "!!!, 0",
            "'nonexistent''; DROP TABLE products; --', 0"
    })
    void shouldHandleSearchSyntaxWithoutTreatingInputAsSql(String query, int expectedCount) {
        seedSearchProduct();

        Page<Product> results = repository.search(query, PageRequest.of(0, 20));

        assertThat(results.getTotalElements()).isEqualTo(expectedCount);
    }

    @Test
    void shouldRankResultsAndPaginateWithSkuAsTieBreaker() {
        repository.saveAllAndFlush(List.of(
                new Product("TEST-A", "comet", null, null, null, Map.of()),
                new Product("TEST-B", "comet", null, null, null, Map.of()),
                new Product("TEST-C", "comet comet comet", null, null, null, Map.of())));

        Page<Product> first = repository.search("comet", PageRequest.of(0, 2));
        Page<Product> second = repository.search("comet", PageRequest.of(1, 2));

        assertThat(first.getContent()).extracting(Product::getSku).containsExactly("TEST-C", "TEST-A");
        assertThat(first.getTotalElements()).isEqualTo(3);
        assertThat(second.getContent()).extracting(Product::getSku).containsExactly("TEST-B");
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
        return repository.search(term, PageRequest.of(0, 20)).getContent().stream()
                .anyMatch(product -> product.getSku().equals(sku));
    }

    private void seedSearchProduct() {
        repository.saveAndFlush(new Product("TEST-SEARCH", "Camping coffee mug", "Keeps drinks warm",
                "Decathlon", "small", Map.of("small", "large",
                "specifications", Map.of("material", "stainless steel"))));
    }

}

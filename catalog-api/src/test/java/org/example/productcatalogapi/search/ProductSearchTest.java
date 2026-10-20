package org.example.productcatalogapi.search;

import java.util.List;
import java.util.Map;

import org.example.productcatalogapi.dto.ProductResponse;
import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
import org.example.productcatalogapi.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=validate")
@Import(ProductService.class)
@Testcontainers
class ProductSearchTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final ProductRepository repository;
    private final JdbcTemplate jdbc;
    private final ProductService service;

    @Autowired
    ProductSearchTest(ProductRepository repository, JdbcTemplate jdbc, ProductService service) {
        this.repository = repository;
        this.jdbc = jdbc;
        this.service = service;
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

    @ParameterizedTest
    @CsvSource({
            "asc,,TEST-B,TEST-A,10003",
            "desc,,TEST-A,TEST-B,10003",
            "asc,sorttest,TEST-B,TEST-A,3",
            "desc,sorttest,TEST-A,TEST-B,3"
    })
    void shouldSortPricesBeforePaginationForBrowsingAndSearching(
            String direction, String query, String firstSku, String secondSku, int total) {
        seedSortableProducts();
        Sort sort = Sort.by(Sort.Direction.fromString(direction), "price");

        Page<ProductResponse> first = service.getProducts(query, 0, 1, sort);
        Page<ProductResponse> second = service.getProducts(query, 1, 1, sort);

        assertThat(first.getContent()).extracting(ProductResponse::sku).containsExactly(firstSku);
        assertThat(second.getContent()).extracting(ProductResponse::sku).containsExactly(secondSku);
        assertThat(first.getTotalElements()).isEqualTo(total);
        assertThat(second.getTotalElements()).isEqualTo(total);
        assertThat(service.getProducts("sorttest", 2, 1, sort).getContent())
                .extracting(ProductResponse::sku).containsExactly("TEST-C");
    }

    @ParameterizedTest
    @CsvSource({
            "sku,TEST-A,TEST-B,TEST-C",
            "name,TEST-B,TEST-C,TEST-A",
            "description,TEST-B,TEST-C,TEST-A",
            "brand,TEST-B,TEST-C,TEST-A",
            "category,TEST-B,TEST-C,TEST-A",
            "attributes,TEST-B,TEST-C,TEST-A",
            "currency,TEST-B,TEST-A,TEST-C",
            "priceUpdatedAt,TEST-B,TEST-A,TEST-C"
    })
    void shouldSupportEveryOtherColumnWithStableSkuTieBreaker(
            String field, String first, String second, String third) {
        seedSortableProducts();

        Page<ProductResponse> results = service.getProducts("sorttest", 0, 20, Sort.by(field));

        assertThat(results.getContent()).extracting(ProductResponse::sku).containsExactly(first, second, third);
    }

    private void seedSortableProducts() {
        repository.saveAllAndFlush(List.of(
                new Product("TEST-A", "Zebra mug", "sorttest Zulu", "Zed", "Travel", Map.of("color", "zebra")),
                new Product("TEST-B", "Apple mug", "sorttest Alpha", "Alpha", "Kitchen", Map.of("color", "apple")),
                new Product("TEST-C", "Apple mug", "sorttest Alpha", "Alpha", "Kitchen", Map.of("color", "apple"))));
        jdbc.update("""
                UPDATE products SET price = 25.00, currency = 'USD', price_updated_at = '2026-01-01T00:00:00Z'
                WHERE sku = 'TEST-A'
                """);
        jdbc.update("""
                UPDATE products SET price = 5.00, currency = 'EUR', price_updated_at = '2025-01-01T00:00:00Z'
                WHERE sku = 'TEST-B'
                """);
    }

    private void seedSearchProduct() {
        repository.saveAndFlush(new Product("TEST-SEARCH", "Camping coffee mug", "Keeps drinks warm",
                "Decathlon", "small", Map.of("small", "large",
                "specifications", Map.of("material", "stainless steel"))));
    }

}

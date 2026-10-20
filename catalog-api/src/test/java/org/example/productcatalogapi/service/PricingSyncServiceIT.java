package org.example.productcatalogapi.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.example.productcatalogapi.dto.PriceChangesResponse.PriceChange;
import org.example.productcatalogapi.model.Product;
import org.example.productcatalogapi.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "pricing.enabled=false")
@Testcontainers
class PricingSyncServiceIT {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00.123456Z");

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:17");

    private final PricingSyncService sync;
    private final ProductRepository products;
    private final JdbcTemplate jdbc;

    @Autowired
    PricingSyncServiceIT(PricingSyncService sync, ProductRepository products, JdbcTemplate jdbc) {
        this.sync = sync;
        this.products = products;
        this.jdbc = jdbc;
    }

    @BeforeEach
    void resetSyncData() {
        jdbc.update("""
                UPDATE products SET price = NULL, currency = NULL, price_updated_at = NULL
                WHERE sku IN ('SKU-000001', 'SKU-000002')
                """);
        jdbc.update("UPDATE sync_state SET last_synced_at = '1970-01-01T00:00:00Z' WHERE name = 'pricing'");
    }

    @Test
    void shouldApplyInitialAndIncrementalPricesWithSourceCursor() {
        assertThat(sync.getCursor()).isEqualTo(Instant.EPOCH);

        sync.applyChanges(Instant.EPOCH, response(NOW, change("SKU-000001", "12.34", NOW)));

        Product initial = products.findById("SKU-000001").orElseThrow();
        assertThat(initial.getPrice()).isEqualByComparingTo("12.34");
        assertThat(initial.getCurrency()).isEqualTo("EUR");
        assertThat(initial.getPriceUpdatedAt()).isEqualTo(NOW);
        assertThat(sync.getCursor()).isEqualTo(NOW);

        sync.applyChanges(NOW, response(NOW.plusSeconds(2), change("SKU-000001", "13.00", NOW.plusSeconds(1))));

        assertThat(products.findById("SKU-000001").orElseThrow().getPrice()).isEqualByComparingTo("13.00");
        assertThat(sync.getCursor()).isEqualTo(NOW.plusSeconds(2));
    }

    @Test
    void shouldIgnoreOlderAndDuplicateUpdates() {
        sync.applyChanges(Instant.EPOCH, response(NOW, change("SKU-000001", "12.34", NOW)));

        sync.applyChanges(NOW, response(NOW.plusSeconds(1),
                change("SKU-000001", "99.00", NOW.minusSeconds(1)),
                change("SKU-000001", "99.00", NOW)));

        Product product = products.findById("SKU-000001").orElseThrow();
        assertThat(product.getPrice()).isEqualByComparingTo("12.34");
        assertThat(product.getPriceUpdatedAt()).isEqualTo(NOW);
        assertThat(sync.getCursor()).isEqualTo(NOW.plusSeconds(1));
    }

    @Test
    void shouldRollBackPricesAndCursorWhenDatabaseWriteFails() {
        PriceChangesResponse response = response(NOW,
                change("SKU-000001", "12.34", NOW),
                change("SKU-000002", "10000000000.00", NOW));

        assertThatThrownBy(() -> sync.applyChanges(Instant.EPOCH, response))
                .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(products.findById("SKU-000001").orElseThrow().getPrice()).isNull();
        assertThat(sync.getCursor()).isEqualTo(Instant.EPOCH);
    }

    @Test
    void shouldRollBackBatchContainingUnknownSku() {
        PriceChangesResponse response = response(NOW,
                change("SKU-000001", "12.34", NOW), change("UNKNOWN", "5.00", NOW));

        assertThatThrownBy(() -> sync.applyChanges(Instant.EPOCH, response))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("unknown SKU");

        assertThat(products.findById("SKU-000001").orElseThrow().getPrice()).isNull();
        assertThat(sync.getCursor()).isEqualTo(Instant.EPOCH);
    }

    private PriceChangesResponse response(Instant watermark, PriceChange... changes) {
        return new PriceChangesResponse(watermark, List.of(changes));
    }

    private PriceChange change(String sku, String price, Instant updatedAt) {
        return new PriceChange(sku, new BigDecimal(price), "EUR", updatedAt);
    }

}

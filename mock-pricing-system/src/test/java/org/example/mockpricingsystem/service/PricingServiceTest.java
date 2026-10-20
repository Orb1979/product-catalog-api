package org.example.mockpricingsystem.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.example.mockpricingsystem.dto.PriceChangesResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");

    private final PricingService service = new PricingService(Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void shouldSeedTenThousandPricesMatchingCatalogSkus() {
        PriceChangesResponse response = service.getChanges(Instant.EPOCH);

        assertThat(response.changes()).hasSize(10_000);
        assertThat(response.changes().getFirst().sku()).isEqualTo("SKU-000001");
        assertThat(response.changes().getLast().sku()).isEqualTo("SKU-010000");
        assertThat(response.changes().getFirst().price()).isEqualByComparingTo("10.01");
        assertThat(response.changes().getFirst().currency()).isEqualTo("EUR");
    }

    @Test
    void shouldReturnOnlyChangedPricesWithoutLosingUpdatesWithinOneClockTick() {
        Instant cursor = service.getChanges(Instant.EPOCH).watermark();

        service.changePrices();
        PriceChangesResponse first = service.getChanges(cursor);
        service.changePrices();
        PriceChangesResponse second = service.getChanges(first.watermark());

        assertThat(first.changes()).hasSize(10);
        assertThat(first.changes().getFirst().price()).isEqualByComparingTo("10.11");
        assertThat(first.changes().getFirst().updatedAt()).isAfter(cursor).isBeforeOrEqualTo(first.watermark());
        assertThat(second.changes()).hasSize(10);
        assertThat(second.changes().getFirst().price()).isEqualByComparingTo("10.21");
        assertThat(second.changes().getFirst().updatedAt()).isAfter(first.watermark());
    }

    @Test
    void shouldReturnNoChangesWhenNothingChangedSinceWatermark() {
        Instant cursor = service.getChanges(Instant.EPOCH).watermark();

        PriceChangesResponse response = service.getChanges(cursor);

        assertThat(response.changes()).isEmpty();
        assertThat(response.watermark()).isAfter(cursor);
    }

}

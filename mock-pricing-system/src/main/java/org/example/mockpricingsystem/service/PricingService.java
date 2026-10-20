package org.example.mockpricingsystem.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.example.mockpricingsystem.dto.PriceChange;
import org.example.mockpricingsystem.dto.PriceChangesResponse;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class PricingService {

    private static final BigDecimal PRICE_INCREMENT = new BigDecimal("0.10");

    private final Clock clock;
    private final Map<String, PriceChange> prices = new LinkedHashMap<>();
    private Instant watermark;

    public PricingService(Clock clock) {
        this.clock = clock;
        this.watermark = clock.instant().truncatedTo(ChronoUnit.MICROS);
        for (int i = 1; i <= 10_000; i++) {
            String sku = String.format(Locale.ROOT, "SKU-%06d", i);
            prices.put(sku, new PriceChange(sku, BigDecimal.valueOf(1000 + i, 2), "EUR", watermark));
        }
    }

    public synchronized PriceChangesResponse getChanges(Instant since) {
        Instant until = advanceWatermark();
        return new PriceChangesResponse(until, prices.values().stream()
                .filter(price -> price.updatedAt().isAfter(since))
                .toList());
    }

    @Scheduled(fixedDelayString = "${pricing.change-interval}",
            initialDelayString = "${pricing.change-interval}")
    public synchronized void changePrices() {
        Instant updatedAt = advanceWatermark();
        for (int i = 1; i <= 10; i++) {
            String sku = String.format(Locale.ROOT, "SKU-%06d", i);
            PriceChange current = prices.get(sku);
            prices.put(sku, new PriceChange(sku, current.price().add(PRICE_INCREMENT), current.currency(), updatedAt));
        }
    }

    private Instant advanceWatermark() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        // Stay ahead of returned cursors, even within one clock tick or after a clock rollback.
        watermark = now.isAfter(watermark) ? now : watermark.plus(1, ChronoUnit.MICROS);
        return watermark;
    }

}

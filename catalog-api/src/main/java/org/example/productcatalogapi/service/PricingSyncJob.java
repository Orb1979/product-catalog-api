package org.example.productcatalogapi.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productcatalogapi.client.PricingClient;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "pricing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PricingSyncJob {

    private final PricingClient client;
    private final PricingSyncService sync;

    @Scheduled(fixedDelayString = "${pricing.sync-interval}", initialDelayString = "${pricing.sync-interval}")
    public void syncPrices() {
        Instant cursor = sync.getCursor();
        PriceChangesResponse response = client.getChanges(cursor);
        sync.applyChanges(cursor, response);
        log.info("Processed pricing response: {} changes, watermark {}", response.changes().size(), response.watermark());
    }

}

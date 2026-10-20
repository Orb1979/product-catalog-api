package org.example.productcatalogapi.service;

import java.time.Instant;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.productcatalogapi.client.PricingClient;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.dao.DataAccessException;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "pricing", name = "enabled", havingValue = "true", matchIfMissing = true)
public class PricingSyncJob {

    private final PricingClient client;
    private final PricingSyncService sync;

    @Scheduled(fixedDelayString = "${pricing.sync-interval}", initialDelayString = "${pricing.sync-interval}")
    public void syncPrices() {
        try {
            Instant cursor = sync.getCursor();
            PriceChangesResponse response = client.getChanges(cursor);
            sync.applyChanges(cursor, response);
            log.info("Processed pricing response: {} changes, watermark {}", response.changes().size(), response.watermark());
        } catch (RestClientException | CallNotPermittedException | DataAccessException | IllegalStateException exception) {
            log.warn("Price sync failed ({}). Will try again on the next scheduled poll.",
                    exception.getClass().getSimpleName());
        }
    }

}

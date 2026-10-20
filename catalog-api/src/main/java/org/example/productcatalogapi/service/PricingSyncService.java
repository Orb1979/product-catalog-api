package org.example.productcatalogapi.service;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.example.productcatalogapi.dto.PriceChangesResponse.PriceChange;
import org.example.productcatalogapi.model.SyncState;
import org.example.productcatalogapi.repository.ProductRepository;
import org.example.productcatalogapi.repository.SyncStateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PricingSyncService {

    private final SyncStateRepository states;
    private final ProductRepository products;

    @Transactional(readOnly = true)
    public Instant getCursor() {
        return states.findById("pricing").orElseThrow().getLastSyncedAt();
    }

    @Transactional
    public void applyChanges(Instant expectedCursor, PriceChangesResponse response) {
        SyncState state = states.lockPricingState().orElseThrow();
        if (response.watermark() == null || response.changes() == null
                || response.watermark().isBefore(expectedCursor)) {
            throw new IllegalStateException("Invalid pricing response watermark or changes.");
        }
        for (PriceChange change : response.changes()) {
            validateChange(change, response.watermark());
            int updated = products.updatePriceIfNewer(change.sku(), change.price(),
                    change.currency(), change.updatedAt());
            if (updated == 0 && !products.existsById(change.sku())) {
                throw new IllegalStateException("Pricing response contains an unknown SKU: " + change.sku());
            }
        }
        state.setLastSyncedAt(response.watermark());
    }

    private void validateChange(PriceChange change, Instant watermark) {
        if (change == null || change.sku() == null || change.price() == null
                || change.price().signum() < 0 || change.currency() == null
                || !change.currency().matches("[A-Z]{3}") || change.updatedAt() == null
                || change.updatedAt().isAfter(watermark)) {
            throw new IllegalStateException("Invalid price change in pricing response.");
        }
    }

}

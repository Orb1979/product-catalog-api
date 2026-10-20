package org.example.productcatalogapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PriceChangesResponse(Instant watermark, List<PriceChange> changes) {

    public record PriceChange(String sku, BigDecimal price, String currency, Instant updatedAt) {
    }

}

package org.example.mockpricingsystem.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceChange(String sku, BigDecimal price, String currency, Instant updatedAt) {
}

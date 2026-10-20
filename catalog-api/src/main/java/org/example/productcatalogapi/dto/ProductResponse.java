package org.example.productcatalogapi.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import org.example.productcatalogapi.model.Product;

public record ProductResponse(
        String sku,
        String name,
        String description,
        String brand,
        String category,
        Map<String, Object> attributes,
        BigDecimal price,
        String currency,
        Instant priceUpdatedAt) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getSku(),
            product.getName(),
            product.getDescription(),
            product.getBrand(),
            product.getCategory(),
            Map.copyOf(product.getAttributes()),
            product.getPrice(),
            product.getCurrency(),
            product.getPriceUpdatedAt()
        );
    }

}

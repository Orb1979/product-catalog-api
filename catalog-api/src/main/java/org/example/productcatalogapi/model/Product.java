package org.example.productcatalogapi.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product {

    @Id
    @Column(length = 32)
    private String sku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String category;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> attributes = Map.of();

    @Column(precision = 12, scale = 2)
    private BigDecimal price;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 3)
    private String currency;

    @Column(name = "price_updated_at")
    private Instant priceUpdatedAt;

    public Product(String sku, String name, String description, String brand,
                   String category, Map<String, Object> attributes) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.brand = brand;
        this.category = category;
        this.attributes = Map.copyOf(attributes);
    }

}

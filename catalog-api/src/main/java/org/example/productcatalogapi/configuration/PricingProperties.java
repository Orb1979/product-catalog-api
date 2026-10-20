package org.example.productcatalogapi.configuration;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(URI baseUrl) {
}

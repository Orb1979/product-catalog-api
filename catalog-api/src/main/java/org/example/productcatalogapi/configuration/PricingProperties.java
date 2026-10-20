package org.example.productcatalogapi.configuration;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pricing")
public record PricingProperties(
		URI baseUrl,
		Duration connectTimeout,
		Duration readTimeout,
		Duration retryDelay,
		Duration circuitOpenDuration) {
}

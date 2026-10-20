package org.example.productcatalogapi.client;

import java.time.Clock;
import java.time.Instant;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.example.productcatalogapi.configuration.PricingProperties;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PricingClient {

    private final RestClient client;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;

    public PricingClient(PricingProperties properties, Clock clock) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeout());
        requestFactory.setReadTimeout(properties.readTimeout());
        this.client = RestClient.builder().baseUrl(properties.baseUrl().toString())
                .requestFactory(requestFactory).build();
        this.retry = Retry.of("pricing", RetryConfig.custom()
                .maxAttempts(3)
                .intervalFunction(IntervalFunction.ofExponentialBackoff(properties.retryDelay()))
                .retryOnException(PricingClient::isTransientFailure)
                .build());
        this.circuitBreaker = CircuitBreaker.of("pricing", CircuitBreakerConfig.custom()
                .slidingWindowSize(3)
                .minimumNumberOfCalls(3)
                .failureRateThreshold(100)
                .waitDurationInOpenState(properties.circuitOpenDuration())
                .permittedNumberOfCallsInHalfOpenState(1)
                .ignoreException(exception -> !isTransientFailure(exception))
                .clock(clock)
                .build());
    }

    public PriceChangesResponse getChanges(Instant since) {
        // Count one failed poll, not each individual retry, towards opening the circuit.
        return circuitBreaker.executeSupplier(() -> retry.executeSupplier(() -> fetchChanges(since)));
    }

    private PriceChangesResponse fetchChanges(Instant since) {
        PriceChangesResponse response = client.get()
                .uri(builder -> builder.path("/api/prices").queryParam("since", since).build())
                .retrieve()
                .body(PriceChangesResponse.class);
        if (response == null) {
            throw new IllegalStateException("Pricing system returned an empty response.");
        }
        return response;
    }

    private static boolean isTransientFailure(Throwable exception) {
        return exception instanceof ResourceAccessException
                || exception instanceof RestClientResponseException response
                && response.getStatusCode().is5xxServerError();
    }

}

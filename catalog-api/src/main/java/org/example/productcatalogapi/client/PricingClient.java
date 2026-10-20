package org.example.productcatalogapi.client;

import java.time.Instant;

import org.example.productcatalogapi.configuration.PricingProperties;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class PricingClient {

    private final RestClient client;

    public PricingClient(PricingProperties properties) {
        this.client = RestClient.create(properties.baseUrl().toString());
    }

    public PriceChangesResponse getChanges(Instant since) {
        PriceChangesResponse response = client.get()
                .uri(builder -> builder.path("/api/prices").queryParam("since", since).build())
                .retrieve()
                .body(PriceChangesResponse.class);
        if (response == null) {
            throw new IllegalStateException("Pricing system returned an empty response.");
        }
        return response;
    }

}

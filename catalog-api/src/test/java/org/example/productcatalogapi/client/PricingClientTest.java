package org.example.productcatalogapi.client;

import java.net.URI;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.example.productcatalogapi.configuration.PricingProperties;
import org.example.productcatalogapi.dto.PriceChangesResponse;
import org.example.productcatalogapi.service.PricingSyncJob;
import org.example.productcatalogapi.service.PricingSyncService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PricingClientTest {

    private static final Instant NOW = Instant.parse("2026-01-01T12:00:00Z");
    private static final String RESPONSE = """
            {"watermark":"2026-01-01T12:00:00Z","changes":[
              {"sku":"SKU-000001","price":12.34,"currency":"EUR","updatedAt":"2026-01-01T12:00:00Z"}
            ]}
            """;

    @RegisterExtension
    static final WireMockExtension SERVER = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort()).build();

    private Clock clock;
    private PricingClient client;

    @BeforeEach
    void setUp() {
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(NOW);
        client = createClient(Duration.ofSeconds(2));
    }

    @Test
    void shouldRecoverAfterTwoServerErrors() {
        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).inScenario("recovery")
                .whenScenarioStateIs(STARTED).willReturn(serverError()).willSetStateTo("second attempt"));
        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).inScenario("recovery")
                .whenScenarioStateIs("second attempt").willReturn(serverError()).willSetStateTo("healthy"));
        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).inScenario("recovery")
                .whenScenarioStateIs("healthy").willReturn(okJson(RESPONSE)));

        PriceChangesResponse response = client.getChanges(Instant.EPOCH);

        assertThat(response.watermark()).isEqualTo(NOW);
        assertThat(response.changes().getFirst().price()).isEqualByComparingTo("12.34");
        SERVER.verify(3, getRequestedFor(urlPathEqualTo("/api/prices"))
                .withQueryParam("since", equalTo(Instant.EPOCH.toString())));
    }

    @Test
    void shouldNotRetryBadRequestsOrCountThemTowardsOpeningCircuit() {
        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).willReturn(aResponse().withStatus(400)));

        for (int poll = 0; poll < 4; poll++) {
            assertThatThrownBy(() -> client.getChanges(Instant.EPOCH))
                    .isInstanceOf(HttpClientErrorException.class);
        }

        SERVER.verify(4, getRequestedFor(urlPathEqualTo("/api/prices")));
    }

    @Test
    @Timeout(10)
    void shouldStopAfterThreeTimeoutsWithoutApplyingPricesOrCursor() {
        SERVER.stubFor(get(urlPathEqualTo("/api/prices"))
                .willReturn(okJson(RESPONSE).withFixedDelay(2000)));
        PricingSyncService sync = mock(PricingSyncService.class);
        when(sync.getCursor()).thenReturn(Instant.EPOCH);
        PricingSyncJob job = new PricingSyncJob(createClient(Duration.ofMillis(200)), sync);

        job.syncPrices();

        verify(sync, never()).applyChanges(any(), any());
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                SERVER.verify(3, getRequestedFor(urlPathEqualTo("/api/prices"))));
    }

    @Test
    void shouldOpenAfterThreeFailedPollsAndRecoverAfterCooldown() {
        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).willReturn(serverError()));
        for (int poll = 0; poll < 3; poll++) {
            assertThatThrownBy(() -> client.getChanges(Instant.EPOCH))
                    .isInstanceOf(HttpServerErrorException.class);
        }

        assertThatThrownBy(() -> client.getChanges(Instant.EPOCH))
                .isInstanceOf(CallNotPermittedException.class);
        SERVER.verify(9, getRequestedFor(urlPathEqualTo("/api/prices")));

        SERVER.stubFor(get(urlPathEqualTo("/api/prices")).willReturn(okJson(RESPONSE)));
        when(clock.instant()).thenReturn(NOW.plusSeconds(31));

        assertThat(client.getChanges(Instant.EPOCH).watermark()).isEqualTo(NOW);
        assertThat(client.getChanges(Instant.EPOCH).watermark()).isEqualTo(NOW);
        SERVER.verify(11, getRequestedFor(urlPathEqualTo("/api/prices")));
    }

    private PricingClient createClient(Duration readTimeout) {
        PricingProperties properties = new PricingProperties(URI.create(SERVER.baseUrl()),
                Duration.ofSeconds(1), readTimeout, Duration.ofMillis(1), Duration.ofSeconds(30));
        return new PricingClient(properties, clock);
    }

}

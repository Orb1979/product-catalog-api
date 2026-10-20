package org.example.mockpricingsystem.controller;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.example.mockpricingsystem.dto.PriceChange;
import org.example.mockpricingsystem.dto.PriceChangesResponse;
import org.example.mockpricingsystem.service.PricingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PricingController.class)
class PricingControllerTest {

    private static final Instant WATERMARK = Instant.parse("2026-01-01T12:00:00Z");

    @MockitoBean
    private PricingService service;

    private final MockMvc mvc;

    @Autowired
    PricingControllerTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void shouldDefaultToEpochForInitialPriceLoad() throws Exception {
        when(service.getChanges(Instant.EPOCH)).thenReturn(new PriceChangesResponse(WATERMARK,
                List.of(new PriceChange("SKU-000001", new BigDecimal("10.01"), "EUR", WATERMARK))));

        mvc.perform(get("/api/prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.watermark").value(WATERMARK.toString()))
                .andExpect(jsonPath("$.changes[0].sku").value("SKU-000001"))
                .andExpect(jsonPath("$.changes[0].price").value(10.01))
                .andExpect(jsonPath("$.changes[0].currency").value("EUR"))
                .andExpect(jsonPath("$.changes[0].updatedAt").value(WATERMARK.toString()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-a-timestamp", "2030-01-01T00:00:00Z"})
    void shouldRejectMalformedOrFutureCursor(String since) throws Exception {
        when(service.getChanges(any())).thenReturn(new PriceChangesResponse(WATERMARK, List.of()));

        mvc.perform(get("/api/prices").param("since", since))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

}

package org.example.mockpricingsystem.controller;

import java.time.Instant;

import lombok.RequiredArgsConstructor;
import org.example.mockpricingsystem.dto.PriceChangesResponse;
import org.example.mockpricingsystem.service.PricingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
public class PricingController {

    private final PricingService service;

    @GetMapping("/api/prices")
    public PriceChangesResponse getChanges(
            @RequestParam(defaultValue = "1970-01-01T00:00:00Z")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant since) {

        PriceChangesResponse response = service.getChanges(since);

        if (since.isAfter(response.watermark())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "since must not be ahead of the pricing system's watermark.");
        }
        return response;
    }

}

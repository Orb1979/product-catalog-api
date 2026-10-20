package org.example.mockpricingsystem.dto;

import java.time.Instant;
import java.util.List;

public record PriceChangesResponse(Instant watermark, List<PriceChange> changes) {
}

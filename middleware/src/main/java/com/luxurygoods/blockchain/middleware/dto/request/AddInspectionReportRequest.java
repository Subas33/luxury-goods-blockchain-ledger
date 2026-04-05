package com.luxurygoods.blockchain.middleware.dto.request;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AddInspectionReportRequest(
        @NotBlank @Size(max = 100) String inspector,
        @NotBlank @Size(max = 50) String status,
        @NotBlank @Size(max = 150) String location,
        @NotBlank @Size(max = 4000) String notes,
        @NotNull OffsetDateTime inspectedAt,
        Map<String, String> metadata) {

    public AddInspectionReportRequest {
        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new TreeMap<>(metadata));
    }
}


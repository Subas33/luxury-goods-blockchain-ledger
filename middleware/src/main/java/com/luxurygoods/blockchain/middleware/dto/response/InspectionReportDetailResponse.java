package com.luxurygoods.blockchain.middleware.dto.response;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;

public record InspectionReportDetailResponse(
        String id,
        String assetId,
        String reportHash,
        String inspector,
        String status,
        String location,
        String notes,
        OffsetDateTime inspectedAt,
        Map<String, String> metadata,
        Instant storedAt,
        boolean ledgerSynced,
        Instant ledgerSyncedAt) {
}

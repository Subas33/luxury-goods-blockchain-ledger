package com.luxurygoods.blockchain.middleware.dto.response;

import java.time.Instant;
import java.time.OffsetDateTime;

public record InspectionReportResponse(
        String id,
        String assetId,
        String reportHash,
        String inspector,
        String status,
        String location,
        OffsetDateTime inspectedAt,
        Instant storedAt,
        boolean ledgerSynced) {
}


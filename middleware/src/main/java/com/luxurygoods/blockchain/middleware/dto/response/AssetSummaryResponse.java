package com.luxurygoods.blockchain.middleware.dto.response;

import java.time.Instant;

public record AssetSummaryResponse(
        String assetId,
        String type,
        String brand,
        String owner,
        String latestInspectionHash,
        Instant createdAt,
        Instant updatedAt,
        long inspectionCount,
        String latestInspectionStatus,
        Instant latestInspectionAt) {
}

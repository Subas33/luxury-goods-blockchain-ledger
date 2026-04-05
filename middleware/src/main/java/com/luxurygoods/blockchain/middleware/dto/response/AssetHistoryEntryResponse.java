package com.luxurygoods.blockchain.middleware.dto.response;

public record AssetHistoryEntryResponse(
        String transactionId,
        String timestamp,
        boolean deleted,
        AssetResponse asset) {
}


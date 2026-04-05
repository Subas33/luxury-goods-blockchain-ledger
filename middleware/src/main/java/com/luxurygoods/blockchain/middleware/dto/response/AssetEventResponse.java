package com.luxurygoods.blockchain.middleware.dto.response;

public record AssetEventResponse(
        String eventType,
        String timestamp,
        String previousOwner,
        String newOwner,
        String reportHash) {
}


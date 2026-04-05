package com.luxurygoods.blockchain.middleware.dto.response;

import java.util.List;

public record AssetResponse(
        String assetId,
        String type,
        String brand,
        String owner,
        String createdAt,
        String updatedAt,
        List<AssetEventResponse> eventHistory) {

    public AssetResponse {
        eventHistory = eventHistory == null ? List.of() : List.copyOf(eventHistory);
    }
}


package com.luxurygoods.blockchain.middleware.dto.response;

import java.util.List;

public record AssetHistoryResponse(
        String assetId,
        List<AssetHistoryEntryResponse> history) {

    public AssetHistoryResponse {
        history = history == null ? List.of() : List.copyOf(history);
    }
}


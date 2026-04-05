package com.luxurygoods.blockchain.middleware.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAssetRequest(
        @NotBlank @Size(max = 100) String assetId,
        @NotBlank @Size(max = 100) String type,
        @NotBlank @Size(max = 100) String brand,
        @NotBlank @Size(max = 100) String owner) {
}


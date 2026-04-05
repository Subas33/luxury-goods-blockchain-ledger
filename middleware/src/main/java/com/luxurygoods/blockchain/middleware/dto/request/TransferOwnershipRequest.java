package com.luxurygoods.blockchain.middleware.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TransferOwnershipRequest(
        @NotBlank @Size(max = 100) String newOwner) {
}


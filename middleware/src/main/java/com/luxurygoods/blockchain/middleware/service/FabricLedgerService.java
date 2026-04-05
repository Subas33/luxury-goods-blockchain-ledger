package com.luxurygoods.blockchain.middleware.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import org.hyperledger.fabric.client.CommitException;
import org.hyperledger.fabric.client.CommitStatusException;
import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.EndorseException;
import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.SubmitException;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luxurygoods.blockchain.middleware.dto.request.CreateAssetRequest;
import com.luxurygoods.blockchain.middleware.dto.response.AssetHistoryEntryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetHistoryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetResponse;
import com.luxurygoods.blockchain.middleware.exception.DuplicateAssetException;
import com.luxurygoods.blockchain.middleware.exception.FabricClientException;
import com.luxurygoods.blockchain.middleware.exception.ResourceNotFoundException;

@Service
public class FabricLedgerService {

    private final Contract contract;
    private final ObjectMapper objectMapper;

    public FabricLedgerService(final Contract contract, final ObjectMapper objectMapper) {
        this.contract = contract;
        this.objectMapper = objectMapper;
    }

    public AssetResponse registerAsset(final CreateAssetRequest request) {
        return readAsset(submit("registerAsset",
                request.assetId(),
                request.type(),
                request.brand(),
                request.owner()));
    }

    public AssetResponse transferOwnership(final String assetId, final String newOwner) {
        return readAsset(submit("transferOwnership", assetId, newOwner));
    }

    public AssetResponse addInspectionReport(final String assetId, final String reportHash) {
        return readAsset(submit("addInspectionReport", assetId, reportHash));
    }

    public AssetHistoryResponse getAssetHistory(final String assetId) {
        byte[] result = evaluate("getAssetHistory", assetId);
        try {
            List<AssetHistoryEntryResponse> history = objectMapper.readValue(
                    result, new TypeReference<List<AssetHistoryEntryResponse>>() {
                    });
            return new AssetHistoryResponse(assetId, history);
        } catch (IOException exception) {
            throw new FabricClientException("Failed to parse asset history returned by chaincode", exception);
        }
    }

    public boolean assetExists(final String assetId) {
        byte[] result = evaluate("assetExists", assetId);
        return Boolean.parseBoolean(new String(result, StandardCharsets.UTF_8));
    }

    private AssetResponse readAsset(final byte[] payload) {
        try {
            return objectMapper.readValue(payload, AssetResponse.class);
        } catch (IOException exception) {
            throw new FabricClientException("Failed to parse asset payload returned by chaincode", exception);
        }
    }

    private byte[] submit(final String transactionName, final String... arguments) {
        try {
            return contract.submitTransaction(transactionName, arguments);
        } catch (EndorseException | SubmitException | CommitStatusException | CommitException exception) {
            throw mapFabricException(transactionName, exception);
        }
    }

    private byte[] evaluate(final String transactionName, final String... arguments) {
        try {
            return contract.evaluateTransaction(transactionName, arguments);
        } catch (GatewayException exception) {
            throw mapFabricException(transactionName, exception);
        }
    }

    private RuntimeException mapFabricException(final String transactionName, final Exception exception) {
        String message = exception.getMessage() == null
                ? "Fabric transaction failed: " + transactionName
                : exception.getMessage();
        String normalizedMessage = message.toLowerCase(Locale.ROOT);

        if (normalizedMessage.contains("asset_already_exists") || normalizedMessage.contains("already exists")) {
            return new DuplicateAssetException(message);
        }

        if (normalizedMessage.contains("asset_not_found")
                || normalizedMessage.contains("does not exist")
                || normalizedMessage.contains("not found")) {
            return new ResourceNotFoundException(message);
        }

        return new FabricClientException("Fabric transaction failed for " + transactionName + ": " + message, exception);
    }
}

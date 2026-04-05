package com.luxurygoods.blockchain.chaincode;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.hyperledger.fabric.contract.Context;
import org.hyperledger.fabric.contract.ContractInterface;
import org.hyperledger.fabric.contract.annotation.Contact;
import org.hyperledger.fabric.contract.annotation.Contract;
import org.hyperledger.fabric.contract.annotation.Default;
import org.hyperledger.fabric.contract.annotation.Info;
import org.hyperledger.fabric.contract.annotation.License;
import org.hyperledger.fabric.contract.annotation.Transaction;
import org.hyperledger.fabric.shim.ChaincodeException;
import org.hyperledger.fabric.shim.ledger.KeyModification;
import org.hyperledger.fabric.shim.ledger.QueryResultsIterator;

import com.owlike.genson.Genson;

@Contract(
        name = "luxuryasset",
        info = @Info(
                title = "Luxury Goods Asset Contract",
                description = "Tracks luxury assets, ownership transfers, and inspection hashes",
                version = "1.0.0",
                license = @License(
                        name = "Apache-2.0",
                        url = "https://www.apache.org/licenses/LICENSE-2.0"),
                contact = @Contact(
                        email = "engineering@example.com",
                        name = "Luxury Goods Blockchain Team",
                        url = "https://example.com")))
@Default
public final class AssetContract implements ContractInterface {

    private final Genson genson = new Genson();

    private enum Errors {
        ASSET_ALREADY_EXISTS,
        ASSET_NOT_FOUND
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public AssetState registerAsset(
            final Context ctx,
            final String assetId,
            final String type,
            final String brand,
            final String owner) {
        if (assetExists(ctx, assetId)) {
            throw new ChaincodeException(
                    String.format("Asset %s already exists", assetId),
                    Errors.ASSET_ALREADY_EXISTS.name());
        }

        String timestamp = currentTransactionTimestamp(ctx);
        List<AssetEvent> events = new ArrayList<>();
        events.add(AssetEvent.registered(timestamp, owner));

        AssetState asset = new AssetState(assetId, type, brand, owner, timestamp, timestamp, events);
        return putAsset(ctx, asset);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public AssetState transferOwnership(final Context ctx, final String assetId, final String newOwner) {
        AssetState existingAsset = getAsset(ctx, assetId);
        String timestamp = currentTransactionTimestamp(ctx);

        List<AssetEvent> updatedEvents = new ArrayList<>(existingAsset.getEventHistory());
        updatedEvents.add(AssetEvent.ownershipTransferred(timestamp, existingAsset.getOwner(), newOwner));

        AssetState updatedAsset = new AssetState(
                existingAsset.getAssetId(),
                existingAsset.getType(),
                existingAsset.getBrand(),
                newOwner,
                existingAsset.getCreatedAt(),
                timestamp,
                updatedEvents);

        return putAsset(ctx, updatedAsset);
    }

    @Transaction(intent = Transaction.TYPE.SUBMIT)
    public AssetState addInspectionReport(final Context ctx, final String assetId, final String reportHash) {
        AssetState existingAsset = getAsset(ctx, assetId);
        String timestamp = currentTransactionTimestamp(ctx);

        List<AssetEvent> updatedEvents = new ArrayList<>(existingAsset.getEventHistory());
        updatedEvents.add(AssetEvent.inspectionAdded(timestamp, reportHash));

        AssetState updatedAsset = new AssetState(
                existingAsset.getAssetId(),
                existingAsset.getType(),
                existingAsset.getBrand(),
                existingAsset.getOwner(),
                existingAsset.getCreatedAt(),
                timestamp,
                updatedEvents);

        return putAsset(ctx, updatedAsset);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public String getAssetHistory(final Context ctx, final String assetId) {
        getAsset(ctx, assetId);

        List<AssetHistoryRecord> history = new ArrayList<>();
        try (QueryResultsIterator<KeyModification> keyModifications = ctx.getStub().getHistoryForKey(assetId)) {
            for (KeyModification modification : keyModifications) {
                AssetState asset = null;
                if (!modification.isDeleted() && modification.getStringValue() != null
                        && !modification.getStringValue().isBlank()) {
                    asset = genson.deserialize(modification.getStringValue(), AssetState.class);
                }

                history.add(new AssetHistoryRecord(
                        modification.getTxId(),
                        formatTimestamp(modification.getTimestamp()),
                        modification.isDeleted(),
                        asset));
            }
        }

        return genson.serialize(history);
    }

    @Transaction(intent = Transaction.TYPE.EVALUATE)
    public boolean assetExists(final Context ctx, final String assetId) {
        String assetJson = ctx.getStub().getStringState(assetId);
        return assetJson != null && !assetJson.isBlank();
    }

    private AssetState getAsset(final Context ctx, final String assetId) {
        String assetJson = ctx.getStub().getStringState(assetId);
        if (assetJson == null || assetJson.isBlank()) {
            throw new ChaincodeException(
                    String.format("Asset %s does not exist", assetId),
                    Errors.ASSET_NOT_FOUND.name());
        }
        return genson.deserialize(assetJson, AssetState.class);
    }

    private AssetState putAsset(final Context ctx, final AssetState asset) {
        ctx.getStub().putStringState(asset.getAssetId(), genson.serialize(asset));
        return asset;
    }

    private String currentTransactionTimestamp(final Context ctx) {
        return formatTimestamp(ctx.getStub().getTxTimestamp());
    }

    private String formatTimestamp(final Instant timestamp) {
        return timestamp.toString();
    }
}

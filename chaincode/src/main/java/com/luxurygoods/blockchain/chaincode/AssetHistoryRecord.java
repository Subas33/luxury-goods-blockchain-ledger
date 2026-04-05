package com.luxurygoods.blockchain.chaincode;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType
public final class AssetHistoryRecord {

    @Property
    private final String transactionId;

    @Property
    private final String timestamp;

    @Property
    private final boolean deleted;

    @Property
    private final AssetState asset;

    public AssetHistoryRecord(
            @JsonProperty("transactionId") final String transactionId,
            @JsonProperty("timestamp") final String timestamp,
            @JsonProperty("deleted") final boolean deleted,
            @JsonProperty("asset") final AssetState asset) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.deleted = deleted;
        this.asset = asset;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public AssetState getAsset() {
        return asset;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AssetHistoryRecord that = (AssetHistoryRecord) object;
        return deleted == that.deleted
                && Objects.equals(transactionId, that.transactionId)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(asset, that.asset);
    }

    @Override
    public int hashCode() {
        return Objects.hash(transactionId, timestamp, deleted, asset);
    }
}


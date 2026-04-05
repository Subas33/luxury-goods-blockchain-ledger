package com.luxurygoods.blockchain.chaincode;

import java.util.List;
import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType
public final class AssetState {

    @Property
    private final String assetId;

    @Property
    private final String type;

    @Property
    private final String brand;

    @Property
    private final String owner;

    @Property
    private final String createdAt;

    @Property
    private final String updatedAt;

    @Property
    private final List<AssetEvent> eventHistory;

    public AssetState(
            @JsonProperty("assetId") final String assetId,
            @JsonProperty("type") final String type,
            @JsonProperty("brand") final String brand,
            @JsonProperty("owner") final String owner,
            @JsonProperty("createdAt") final String createdAt,
            @JsonProperty("updatedAt") final String updatedAt,
            @JsonProperty("eventHistory") final List<AssetEvent> eventHistory) {
        this.assetId = assetId;
        this.type = type;
        this.brand = brand;
        this.owner = owner;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.eventHistory = eventHistory;
    }

    public String getAssetId() {
        return assetId;
    }

    public String getType() {
        return type;
    }

    public String getBrand() {
        return brand;
    }

    public String getOwner() {
        return owner;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public List<AssetEvent> getEventHistory() {
        return eventHistory;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AssetState that = (AssetState) object;
        return Objects.equals(assetId, that.assetId)
                && Objects.equals(type, that.type)
                && Objects.equals(brand, that.brand)
                && Objects.equals(owner, that.owner)
                && Objects.equals(createdAt, that.createdAt)
                && Objects.equals(updatedAt, that.updatedAt)
                && Objects.equals(eventHistory, that.eventHistory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(assetId, type, brand, owner, createdAt, updatedAt, eventHistory);
    }
}


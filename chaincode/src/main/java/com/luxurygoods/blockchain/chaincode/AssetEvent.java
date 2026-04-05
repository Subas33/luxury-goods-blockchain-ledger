package com.luxurygoods.blockchain.chaincode;

import java.util.Objects;

import org.hyperledger.fabric.contract.annotation.DataType;
import org.hyperledger.fabric.contract.annotation.Property;

import com.owlike.genson.annotation.JsonProperty;

@DataType
public final class AssetEvent {

    @Property
    private final String eventType;

    @Property
    private final String timestamp;

    @Property
    private final String previousOwner;

    @Property
    private final String newOwner;

    @Property
    private final String reportHash;

    public AssetEvent(
            @JsonProperty("eventType") final String eventType,
            @JsonProperty("timestamp") final String timestamp,
            @JsonProperty("previousOwner") final String previousOwner,
            @JsonProperty("newOwner") final String newOwner,
            @JsonProperty("reportHash") final String reportHash) {
        this.eventType = eventType;
        this.timestamp = timestamp;
        this.previousOwner = previousOwner;
        this.newOwner = newOwner;
        this.reportHash = reportHash;
    }

    public static AssetEvent registered(final String timestamp, final String owner) {
        return new AssetEvent("REGISTERED", timestamp, null, owner, null);
    }

    public static AssetEvent ownershipTransferred(
            final String timestamp,
            final String previousOwner,
            final String newOwner) {
        return new AssetEvent("OWNERSHIP_TRANSFERRED", timestamp, previousOwner, newOwner, null);
    }

    public static AssetEvent inspectionAdded(final String timestamp, final String reportHash) {
        return new AssetEvent("INSPECTION_RECORDED", timestamp, null, null, reportHash);
    }

    public String getEventType() {
        return eventType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getPreviousOwner() {
        return previousOwner;
    }

    public String getNewOwner() {
        return newOwner;
    }

    public String getReportHash() {
        return reportHash;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        AssetEvent that = (AssetEvent) object;
        return Objects.equals(eventType, that.eventType)
                && Objects.equals(timestamp, that.timestamp)
                && Objects.equals(previousOwner, that.previousOwner)
                && Objects.equals(newOwner, that.newOwner)
                && Objects.equals(reportHash, that.reportHash);
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventType, timestamp, previousOwner, newOwner, reportHash);
    }
}


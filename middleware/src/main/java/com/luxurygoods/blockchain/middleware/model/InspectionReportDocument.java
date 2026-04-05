package com.luxurygoods.blockchain.middleware.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "inspection_reports")
public class InspectionReportDocument {

    @Id
    private String id;

    @Indexed
    private String assetId;

    @Indexed
    private String reportHash;

    private String inspector;

    private String status;

    private String location;

    private String notes;

    private Instant inspectedAt;

    private Map<String, String> metadata;

    private Instant storedAt;

    private boolean ledgerSynced;

    private Instant ledgerSyncedAt;

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getAssetId() {
        return assetId;
    }

    public void setAssetId(final String assetId) {
        this.assetId = assetId;
    }

    public String getReportHash() {
        return reportHash;
    }

    public void setReportHash(final String reportHash) {
        this.reportHash = reportHash;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(final String inspector) {
        this.inspector = inspector;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(final String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(final String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(final String notes) {
        this.notes = notes;
    }

    public Instant getInspectedAt() {
        return inspectedAt;
    }

    public void setInspectedAt(final Instant inspectedAt) {
        this.inspectedAt = inspectedAt;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }

    public void setMetadata(final Map<String, String> metadata) {
        this.metadata = metadata;
    }

    public Instant getStoredAt() {
        return storedAt;
    }

    public void setStoredAt(final Instant storedAt) {
        this.storedAt = storedAt;
    }

    public boolean isLedgerSynced() {
        return ledgerSynced;
    }

    public void setLedgerSynced(final boolean ledgerSynced) {
        this.ledgerSynced = ledgerSynced;
    }

    public Instant getLedgerSyncedAt() {
        return ledgerSyncedAt;
    }

    public void setLedgerSyncedAt(final Instant ledgerSyncedAt) {
        this.ledgerSyncedAt = ledgerSyncedAt;
    }
}


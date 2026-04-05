package com.luxurygoods.blockchain.middleware.service;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import org.springframework.stereotype.Service;

import com.luxurygoods.blockchain.middleware.dto.request.AddInspectionReportRequest;
import com.luxurygoods.blockchain.middleware.dto.request.CreateAssetRequest;
import com.luxurygoods.blockchain.middleware.dto.request.TransferOwnershipRequest;
import com.luxurygoods.blockchain.middleware.dto.response.AssetHistoryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetSummaryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.InspectionReportDetailResponse;
import com.luxurygoods.blockchain.middleware.dto.response.InspectionReportResponse;
import com.luxurygoods.blockchain.middleware.exception.ResourceNotFoundException;
import com.luxurygoods.blockchain.middleware.model.AssetDocument;
import com.luxurygoods.blockchain.middleware.model.InspectionReportDocument;
import com.luxurygoods.blockchain.middleware.repository.AssetRepository;
import com.luxurygoods.blockchain.middleware.repository.InspectionReportRepository;

@Service
public class AssetService {

    private final AssetRepository assetRepository;
    private final InspectionReportRepository inspectionReportRepository;
    private final FabricLedgerService fabricLedgerService;
    private final InspectionHashService inspectionHashService;

    public AssetService(
            final AssetRepository assetRepository,
            final InspectionReportRepository inspectionReportRepository,
            final FabricLedgerService fabricLedgerService,
            final InspectionHashService inspectionHashService) {
        this.assetRepository = assetRepository;
        this.inspectionReportRepository = inspectionReportRepository;
        this.fabricLedgerService = fabricLedgerService;
        this.inspectionHashService = inspectionHashService;
    }

    public AssetResponse registerAsset(final CreateAssetRequest request) {
        assetRepository.findByAssetId(request.assetId()).ifPresent(existingDocument -> {
            assetRepository.delete(existingDocument);
            inspectionReportRepository.deleteByAssetId(request.assetId());
        });

        AssetResponse assetResponse = fabricLedgerService.registerAsset(request);
        upsertAssetDocument(assetResponse, null);
        return assetResponse;
    }

    public AssetResponse transferOwnership(final String assetId, final TransferOwnershipRequest request) {
        AssetResponse assetResponse = fabricLedgerService.transferOwnership(assetId, request.newOwner());
        upsertAssetDocument(assetResponse, null);
        return assetResponse;
    }

    public InspectionReportResponse addInspectionReport(
            final String assetId,
            final AddInspectionReportRequest request) {
        String reportHash = inspectionHashService.generateHash(request);

        InspectionReportDocument inspectionDocument = new InspectionReportDocument();
        inspectionDocument.setAssetId(assetId);
        inspectionDocument.setReportHash(reportHash);
        inspectionDocument.setInspector(request.inspector());
        inspectionDocument.setStatus(request.status());
        inspectionDocument.setLocation(request.location());
        inspectionDocument.setNotes(request.notes());
        inspectionDocument.setInspectedAt(request.inspectedAt().toInstant());
        inspectionDocument.setMetadata(request.metadata());
        inspectionDocument.setStoredAt(Instant.now());
        inspectionDocument.setLedgerSynced(false);

        inspectionDocument = inspectionReportRepository.save(inspectionDocument);

        AssetResponse updatedAsset = fabricLedgerService.addInspectionReport(assetId, reportHash);
        inspectionDocument.setLedgerSynced(true);
        inspectionDocument.setLedgerSyncedAt(Instant.now());
        inspectionDocument = inspectionReportRepository.save(inspectionDocument);

        upsertAssetDocument(updatedAsset, reportHash);
        return toInspectionReportResponse(inspectionDocument);
    }

    public AssetHistoryResponse getAssetHistory(final String assetId) {
        return fabricLedgerService.getAssetHistory(assetId);
    }

    public List<AssetSummaryResponse> listAssets() {
        return assetRepository.findAllByOrderByUpdatedAtDesc().stream()
                .filter(document -> fabricLedgerService.assetExists(document.getAssetId()))
                .map(this::toAssetSummary)
                .toList();
    }

    public AssetSummaryResponse getAsset(final String assetId) {
        ensureLedgerAssetExists(assetId);
        AssetDocument document = assetRepository.findByAssetId(assetId)
                .orElseThrow(() -> new ResourceNotFoundException("Asset %s does not exist".formatted(assetId)));
        return toAssetSummary(document);
    }

    public List<InspectionReportDetailResponse> getInspectionReports(final String assetId) {
        ensureLedgerAssetExists(assetId);
        return inspectionReportRepository.findByAssetIdOrderByInspectedAtDesc(assetId).stream()
                .map(this::toInspectionReportDetailResponse)
                .toList();
    }

    private void upsertAssetDocument(final AssetResponse assetResponse, final String latestInspectionHash) {
        AssetDocument document = assetRepository.findByAssetId(assetResponse.assetId())
                .orElseGet(AssetDocument::new);

        document.setAssetId(assetResponse.assetId());
        document.setType(assetResponse.type());
        document.setBrand(assetResponse.brand());
        document.setOwner(assetResponse.owner());
        document.setCreatedAt(parseInstant(assetResponse.createdAt()));
        document.setUpdatedAt(parseInstant(assetResponse.updatedAt()));

        if (latestInspectionHash != null) {
            document.setLatestInspectionHash(latestInspectionHash);
        }

        assetRepository.save(document);
    }

    private void ensureLedgerAssetExists(final String assetId) {
        if (!fabricLedgerService.assetExists(assetId)) {
            throw new ResourceNotFoundException("Asset %s does not exist".formatted(assetId));
        }
    }

    private InspectionReportResponse toInspectionReportResponse(final InspectionReportDocument document) {
        OffsetDateTime inspectedAt = OffsetDateTime.ofInstant(document.getInspectedAt(), ZoneOffset.UTC);
        return new InspectionReportResponse(
                document.getId(),
                document.getAssetId(),
                document.getReportHash(),
                document.getInspector(),
                document.getStatus(),
                document.getLocation(),
                inspectedAt,
                document.getStoredAt(),
                document.isLedgerSynced());
    }

    private InspectionReportDetailResponse toInspectionReportDetailResponse(final InspectionReportDocument document) {
        OffsetDateTime inspectedAt = OffsetDateTime.ofInstant(document.getInspectedAt(), ZoneOffset.UTC);
        return new InspectionReportDetailResponse(
                document.getId(),
                document.getAssetId(),
                document.getReportHash(),
                document.getInspector(),
                document.getStatus(),
                document.getLocation(),
                document.getNotes(),
                inspectedAt,
                document.getMetadata(),
                document.getStoredAt(),
                document.isLedgerSynced(),
                document.getLedgerSyncedAt());
    }

    private AssetSummaryResponse toAssetSummary(final AssetDocument document) {
        InspectionReportDocument latestInspection = inspectionReportRepository
                .findFirstByAssetIdOrderByInspectedAtDesc(document.getAssetId())
                .orElse(null);

        return new AssetSummaryResponse(
                document.getAssetId(),
                document.getType(),
                document.getBrand(),
                document.getOwner(),
                document.getLatestInspectionHash(),
                document.getCreatedAt(),
                document.getUpdatedAt(),
                inspectionReportRepository.countByAssetId(document.getAssetId()),
                latestInspection == null ? null : latestInspection.getStatus(),
                latestInspection == null ? null : latestInspection.getInspectedAt());
    }

    private Instant parseInstant(final String value) {
        return value == null || value.isBlank() ? Instant.now() : Instant.parse(value);
    }
}

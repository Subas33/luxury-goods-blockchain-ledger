package com.luxurygoods.blockchain.middleware.controller;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.luxurygoods.blockchain.middleware.dto.request.AddInspectionReportRequest;
import com.luxurygoods.blockchain.middleware.dto.request.CreateAssetRequest;
import com.luxurygoods.blockchain.middleware.dto.request.TransferOwnershipRequest;
import com.luxurygoods.blockchain.middleware.dto.response.AssetHistoryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetResponse;
import com.luxurygoods.blockchain.middleware.dto.response.AssetSummaryResponse;
import com.luxurygoods.blockchain.middleware.dto.response.InspectionReportDetailResponse;
import com.luxurygoods.blockchain.middleware.dto.response.InspectionReportResponse;
import com.luxurygoods.blockchain.middleware.service.AssetService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(final AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ResponseEntity<java.util.List<AssetSummaryResponse>> listAssets() {
        return ResponseEntity.ok(assetService.listAssets());
    }

    @PostMapping
    public ResponseEntity<AssetResponse> registerAsset(@Valid @RequestBody final CreateAssetRequest request) {
        AssetResponse response = assetService.registerAsset(request);
        return ResponseEntity
                .created(URI.create("/assets/" + response.assetId()))
                .body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AssetSummaryResponse> getAsset(
            @PathVariable("id") @NotBlank final String assetId) {
        return ResponseEntity.ok(assetService.getAsset(assetId));
    }

    @PostMapping("/{id}/transfer")
    public ResponseEntity<AssetResponse> transferOwnership(
            @PathVariable("id") @NotBlank final String assetId,
            @Valid @RequestBody final TransferOwnershipRequest request) {
        return ResponseEntity.ok(assetService.transferOwnership(assetId, request));
    }

    @PostMapping("/{id}/inspection")
    public ResponseEntity<InspectionReportResponse> addInspectionReport(
            @PathVariable("id") @NotBlank final String assetId,
            @Valid @RequestBody final AddInspectionReportRequest request) {
        InspectionReportResponse response = assetService.addInspectionReport(assetId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/assets/" + assetId + "/inspection/" + response.id()))
                .body(response);
    }

    @GetMapping("/{id}/inspections")
    public ResponseEntity<java.util.List<InspectionReportDetailResponse>> getInspectionReports(
            @PathVariable("id") @NotBlank final String assetId) {
        return ResponseEntity.ok(assetService.getInspectionReports(assetId));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<AssetHistoryResponse> getAssetHistory(
            @PathVariable("id") @NotBlank final String assetId) {
        return ResponseEntity.ok(assetService.getAssetHistory(assetId));
    }
}

package com.luxurygoods.blockchain.middleware.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.luxurygoods.blockchain.middleware.model.InspectionReportDocument;

public interface InspectionReportRepository extends MongoRepository<InspectionReportDocument, String> {

    long countByAssetId(String assetId);

    void deleteByAssetId(String assetId);

    Optional<InspectionReportDocument> findFirstByAssetIdOrderByInspectedAtDesc(String assetId);

    List<InspectionReportDocument> findByAssetIdOrderByInspectedAtDesc(String assetId);
}

package com.luxurygoods.blockchain.middleware.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.luxurygoods.blockchain.middleware.model.AssetDocument;

public interface AssetRepository extends MongoRepository<AssetDocument, String> {

    List<AssetDocument> findAllByOrderByUpdatedAtDesc();

    Optional<AssetDocument> findByAssetId(String assetId);
}

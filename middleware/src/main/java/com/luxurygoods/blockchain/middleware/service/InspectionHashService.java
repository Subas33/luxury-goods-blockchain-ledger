package com.luxurygoods.blockchain.middleware.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luxurygoods.blockchain.middleware.dto.request.AddInspectionReportRequest;

@Service
public class InspectionHashService {

    private final ObjectWriter canonicalWriter;

    public InspectionHashService() {
        ObjectMapper canonicalObjectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true)
                .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.canonicalWriter = canonicalObjectMapper.writer();
    }

    public String generateHash(final AddInspectionReportRequest request) {
        try {
            String canonicalPayload = canonicalWriter.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalPayload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to hash inspection report payload", exception);
        }
    }
}


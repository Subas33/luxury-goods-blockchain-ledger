package com.luxurygoods.blockchain.middleware.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "fabric")
public class FabricGatewayProperties {

    @NotBlank
    private String mspId = "Org1MSP";

    @NotBlank
    private String channel = "mychannel";

    @NotBlank
    private String chaincodeName = "luxuryasset";

    @NotBlank
    private String peerEndpoint = "localhost:7051";

    @NotBlank
    private String peerHostAlias = "peer0.org1.example.com";

    @NotNull
    private Path certificateDirectory = Paths.get(
            "../.fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/signcerts");

    @NotNull
    private Path privateKeyDirectory = Paths.get(
            "../.fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/users/User1@org1.example.com/msp/keystore");

    @NotNull
    private Path tlsCertificatePath = Paths.get(
            "../.fabric/fabric-samples/test-network/organizations/peerOrganizations/org1.example.com/peers/peer0.org1.example.com/tls/ca.crt");

    @NotNull
    private Duration evaluateTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration endorseTimeout = Duration.ofSeconds(15);

    @NotNull
    private Duration submitTimeout = Duration.ofSeconds(5);

    @NotNull
    private Duration commitStatusTimeout = Duration.ofSeconds(60);

    public String getMspId() {
        return mspId;
    }

    public void setMspId(final String mspId) {
        this.mspId = mspId;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(final String channel) {
        this.channel = channel;
    }

    public String getChaincodeName() {
        return chaincodeName;
    }

    public void setChaincodeName(final String chaincodeName) {
        this.chaincodeName = chaincodeName;
    }

    public String getPeerEndpoint() {
        return peerEndpoint;
    }

    public void setPeerEndpoint(final String peerEndpoint) {
        this.peerEndpoint = peerEndpoint;
    }

    public String getPeerHostAlias() {
        return peerHostAlias;
    }

    public void setPeerHostAlias(final String peerHostAlias) {
        this.peerHostAlias = peerHostAlias;
    }

    public Path getCertificateDirectory() {
        return certificateDirectory;
    }

    public void setCertificateDirectory(final Path certificateDirectory) {
        this.certificateDirectory = certificateDirectory;
    }

    public Path getPrivateKeyDirectory() {
        return privateKeyDirectory;
    }

    public void setPrivateKeyDirectory(final Path privateKeyDirectory) {
        this.privateKeyDirectory = privateKeyDirectory;
    }

    public Path getTlsCertificatePath() {
        return tlsCertificatePath;
    }

    public void setTlsCertificatePath(final Path tlsCertificatePath) {
        this.tlsCertificatePath = tlsCertificatePath;
    }

    public Duration getEvaluateTimeout() {
        return evaluateTimeout;
    }

    public void setEvaluateTimeout(final Duration evaluateTimeout) {
        this.evaluateTimeout = evaluateTimeout;
    }

    public Duration getEndorseTimeout() {
        return endorseTimeout;
    }

    public void setEndorseTimeout(final Duration endorseTimeout) {
        this.endorseTimeout = endorseTimeout;
    }

    public Duration getSubmitTimeout() {
        return submitTimeout;
    }

    public void setSubmitTimeout(final Duration submitTimeout) {
        this.submitTimeout = submitTimeout;
    }

    public Duration getCommitStatusTimeout() {
        return commitStatusTimeout;
    }

    public void setCommitStatusTimeout(final Duration commitStatusTimeout) {
        this.commitStatusTimeout = commitStatusTimeout;
    }
}

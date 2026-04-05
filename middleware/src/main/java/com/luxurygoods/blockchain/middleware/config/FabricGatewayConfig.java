package com.luxurygoods.blockchain.middleware.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.cert.CertificateException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.hyperledger.fabric.client.Contract;
import org.hyperledger.fabric.client.Gateway;
import org.hyperledger.fabric.client.Hash;
import org.hyperledger.fabric.client.identity.Identities;
import org.hyperledger.fabric.client.identity.Identity;
import org.hyperledger.fabric.client.identity.Signer;
import org.hyperledger.fabric.client.identity.Signers;
import org.hyperledger.fabric.client.identity.X509Identity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.grpc.ChannelCredentials;
import io.grpc.Grpc;
import io.grpc.ManagedChannel;
import io.grpc.TlsChannelCredentials;

@Configuration
public class FabricGatewayConfig {

    @Bean(destroyMethod = "shutdownNow")
    public ManagedChannel fabricManagedChannel(final FabricGatewayProperties properties) throws IOException {
        ensureReadable(properties.getTlsCertificatePath());

        ChannelCredentials credentials = TlsChannelCredentials.newBuilder()
                .trustManager(properties.getTlsCertificatePath().toFile())
                .build();

        return Grpc.newChannelBuilder(properties.getPeerEndpoint(), credentials)
                .overrideAuthority(properties.getPeerHostAlias())
                .build();
    }

    @Bean
    public Identity fabricIdentity(final FabricGatewayProperties properties) throws IOException, CertificateException {
        Path certificatePath = firstFile(properties.getCertificateDirectory());
        try (BufferedReader certReader = Files.newBufferedReader(certificatePath)) {
            return new X509Identity(properties.getMspId(), Identities.readX509Certificate(certReader));
        }
    }

    @Bean
    public Signer fabricSigner(final FabricGatewayProperties properties) throws IOException, InvalidKeyException {
        Path privateKeyPath = firstFile(properties.getPrivateKeyDirectory());
        try (BufferedReader privateKeyReader = Files.newBufferedReader(privateKeyPath)) {
            return Signers.newPrivateKeySigner(Identities.readPrivateKey(privateKeyReader));
        }
    }

    @Bean(destroyMethod = "close")
    public Gateway fabricGateway(
            final ManagedChannel fabricManagedChannel,
            final Identity fabricIdentity,
            final Signer fabricSigner,
            final FabricGatewayProperties properties) {
        return Gateway.newInstance()
                .identity(fabricIdentity)
                .signer(fabricSigner)
                .hash(Hash.SHA256)
                .connection(fabricManagedChannel)
                .evaluateOptions(options -> options.withDeadlineAfter(
                        properties.getEvaluateTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .endorseOptions(options -> options.withDeadlineAfter(
                        properties.getEndorseTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .submitOptions(options -> options.withDeadlineAfter(
                        properties.getSubmitTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .commitStatusOptions(options -> options.withDeadlineAfter(
                        properties.getCommitStatusTimeout().toMillis(), TimeUnit.MILLISECONDS))
                .connect();
    }

    @Bean
    public Contract fabricContract(final Gateway fabricGateway, final FabricGatewayProperties properties) {
        return fabricGateway.getNetwork(properties.getChannel()).getContract(properties.getChaincodeName());
    }

    private Path firstFile(final Path directory) throws IOException {
        ensureReadable(directory);
        try (Stream<Path> files = Files.list(directory)) {
            return files.sorted().findFirst()
                    .orElseThrow(() -> new IllegalStateException("No files found in " + directory.toAbsolutePath()));
        }
    }

    private void ensureReadable(final Path path) {
        if (!Files.exists(path) || !Files.isReadable(path)) {
            throw new IllegalStateException("Fabric credential path does not exist: " + path.toAbsolutePath());
        }
    }
}

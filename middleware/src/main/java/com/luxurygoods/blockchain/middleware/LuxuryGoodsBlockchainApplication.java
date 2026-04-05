package com.luxurygoods.blockchain.middleware;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.luxurygoods.blockchain.middleware.config.FabricGatewayProperties;

@SpringBootApplication
@EnableConfigurationProperties(FabricGatewayProperties.class)
public class LuxuryGoodsBlockchainApplication {

    public static void main(final String[] args) {
        SpringApplication.run(LuxuryGoodsBlockchainApplication.class, args);
    }
}


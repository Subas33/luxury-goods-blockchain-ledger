package com.luxurygoods.blockchain.middleware.exception;

public class FabricClientException extends RuntimeException {

    public FabricClientException(final String message, final Throwable cause) {
        super(message, cause);
    }
}


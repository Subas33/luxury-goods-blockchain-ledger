package com.luxurygoods.blockchain.middleware.exception;

public class DuplicateAssetException extends RuntimeException {

    public DuplicateAssetException(final String message) {
        super(message);
    }
}


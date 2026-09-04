package com.marketpulse.common.exception;

import org.springframework.http.HttpStatus;

public class InvalidSymbolException extends ApiException {
    public InvalidSymbolException(String symbol) {
        super(HttpStatus.BAD_REQUEST, "INVALID_SYMBOL", "Symbol '" + symbol + "' is not valid");
    }
}

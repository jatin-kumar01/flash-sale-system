package com.flashsale.common.exception;

public class InvalidRequestException extends BaseCustomException {

    private static final int STATUS_CODE = 400;
    private static final String ERROR_CODE = "INVALID_REQUEST";

    public InvalidRequestException(String message) {
        super(message, STATUS_CODE, ERROR_CODE);
    }
}

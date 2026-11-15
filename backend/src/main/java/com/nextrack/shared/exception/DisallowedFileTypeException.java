package com.nextrack.shared.exception;

public class DisallowedFileTypeException extends RuntimeException {

    public DisallowedFileTypeException(String message) {
        super(message);
    }
}

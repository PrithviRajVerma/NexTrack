package com.nextrack.development.application.service;

public class StaleLeaseException extends RuntimeException {
    public StaleLeaseException(String message) {
        super(message);
    }
}

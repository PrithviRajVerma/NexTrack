package com.nextrack.shared.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * RFC 7807 / RFC 9457 Problem Details for HTTP APIs.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
        String type,
        String title,
        int status,
        String detail,
        String code,
        String instance,
        Instant timestamp,
        List<ValidationError> errors
) {
    public static ApiErrorResponse of(int status, String title, String detail, String code, String instance) {
        return new ApiErrorResponse(
                "https://api.nextrack.io/errors/" + code.toLowerCase().replace('_', '-'),
                title,
                status,
                detail,
                code,
                instance,
                Instant.now(),
                null
        );
    }

    public static ApiErrorResponse withValidationErrors(int status, String detail, String instance, List<ValidationError> errors) {
        return new ApiErrorResponse(
                "https://api.nextrack.io/errors/validation-error",
                "Validation Error",
                status,
                detail,
                "VALIDATION_ERROR",
                instance,
                Instant.now(),
                errors
        );
    }

    public record ValidationError(String field, String message) {}
}

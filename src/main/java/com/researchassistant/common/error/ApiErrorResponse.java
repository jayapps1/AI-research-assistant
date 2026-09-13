package com.researchassistant.common.error;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Standard JSON error structure returned by the REST API.
 *
 * <p>Using one consistent error contract allows the React web
 * client and Android application to process backend failures
 * predictably.</p>
 *
 * @param timestamp time the error response was produced
 * @param status HTTP status code
 * @param error short HTTP error description
 * @param message human-readable application message
 * @param path request path that caused the error
 * @param validationErrors field-level validation failures
 */
public record ApiErrorResponse(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> validationErrors
) {
}
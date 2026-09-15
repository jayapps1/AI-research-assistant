package com.researchassistant.common.error;

import com.researchassistant.common.exception.AuthenticationFailedException;
import com.researchassistant.common.exception.DuplicateResourceException;
import com.researchassistant.common.exception.ResourceNotFoundException;
import com.researchassistant.collaboration.exception.ArtifactVersionConflictException;
import com.researchassistant.document.exception.DocumentAccessDeniedException;
import com.researchassistant.document.exception.DocumentStorageException;
import com.researchassistant.document.exception.DocumentUploadException;
import com.researchassistant.document.exception.InvalidDocumentOperationException;
import com.researchassistant.document.exception.UnsupportedDocumentTypeException;
import com.researchassistant.project.exception.InvalidProjectOperationException;
import com.researchassistant.project.exception.ProjectAccessDeniedException;
import com.researchassistant.methodology.exception.MethodologyValidationException;
import com.researchassistant.rag.exception.RagAccessDeniedException;
import com.researchassistant.rag.exception.RagCapabilityUnavailableException;
import com.researchassistant.rag.exception.RagVerificationException;
import com.researchassistant.workspace.exception.InvalidWorkspaceOperationException;
import com.researchassistant.workspace.exception.WorkspaceAccessDeniedException;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.MDC;

/**
 * Central exception handler for REST API requests.
 *
 * <p>Controllers should generally allow domain and validation
 * exceptions to propagate. This component converts those
 * exceptions into consistent HTTP responses for clients.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Converts authentication and token refresh failures into
     * HTTP 401 without exposing credential or token internals.
     */
    @ExceptionHandler(AuthenticationFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthenticationFailed(
            AuthenticationFailedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    /**
     * Converts duplicate-resource failures into HTTP 409 Conflict.
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiErrorResponse> handleDuplicateResource(
            DuplicateResourceException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    /**
     * Converts missing-resource failures into HTTP 404 Not Found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleWorkspaceAccessDenied(
            WorkspaceAccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(InvalidWorkspaceOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidWorkspaceOperation(
            InvalidWorkspaceOperationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(ProjectAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleProjectAccessDenied(
            ProjectAccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(InvalidProjectOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidProjectOperation(
            InvalidProjectOperationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(DocumentAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentAccessDenied(
            DocumentAccessDeniedException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(InvalidDocumentOperationException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidDocumentOperation(
            InvalidDocumentOperationException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ResponseEntity<ApiErrorResponse> handleUnsupportedDocumentType(
            UnsupportedDocumentTypeException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(DocumentUploadException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentUpload(
            DocumentUploadException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.PAYLOAD_TOO_LARGE,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(DocumentStorageException.class)
    public ResponseEntity<ApiErrorResponse> handleDocumentStorage(
            DocumentStorageException exception,
            HttpServletRequest request
    ) {

        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Document storage failed.",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(RagAccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleRagAccessDenied(
            RagAccessDeniedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(RagCapabilityUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleRagCapabilityUnavailable(
            RagCapabilityUnavailableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(com.researchassistant.ai.exception.AiRateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleAiRateLimitExceeded(
            com.researchassistant.ai.exception.AiRateLimitExceededException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(com.researchassistant.ai.exception.AiPolicyViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleAiPolicyViolation(
            com.researchassistant.ai.exception.AiPolicyViolationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(RagVerificationException.class)
    public ResponseEntity<ApiErrorResponse> handleRagVerification(
            RagVerificationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(MethodologyValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodologyValidation(
            MethodologyValidationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(ArtifactVersionConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleArtifactVersionConflict(
            ArtifactVersionConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "artifactId", exception.artifactId().toString(),
                        "expectedVersion", String.valueOf(exception.expectedVersion()),
                        "currentVersion", String.valueOf(exception.currentVersion())
                )
        );
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(com.researchassistant.usage.QuotaExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleQuotaExceeded(
            com.researchassistant.usage.QuotaExceededException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "QUOTA_EXCEEDED",
                request.getRequestURI(),
                Map.of(
                        "feature", exception.feature().name(),
                        "limit", String.valueOf(exception.limit()),
                        "used", String.valueOf(exception.used()),
                        "remaining", String.valueOf(exception.remaining()),
                        "resetAt", exception.resetAt().toString()
                )
        );
    }

    @ExceptionHandler(com.researchassistant.subscription.FeatureNotEntitledException.class)
    public ResponseEntity<ApiErrorResponse> handleFeatureNotEntitled(
            com.researchassistant.subscription.FeatureNotEntitledException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                "Feature is not enabled for the current subscription.",
                request.getRequestURI(),
                Map.of("feature", exception.feature().name())
        );
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalState(
            IllegalStateException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    /**
     * Handles request-body validation failures.
     *
     * <p>Every invalid field is returned using a predictable
     * field-to-message structure so web and Android clients can
     * display validation feedback directly beside form inputs.</p>
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> validationErrors =
                new LinkedHashMap<>();

        for (FieldError fieldError
                : exception.getBindingResult().getFieldErrors()) {

            validationErrors.putIfAbsent(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request.getRequestURI(),
                validationErrors
        );
    }


    /**
     * Builds the standard API error response.
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {

        ApiErrorResponse body = new ApiErrorResponse(
                "about:blank",
                status.getReasonPhrase(),
                OffsetDateTime.now(),
                MDC.get("requestId"),
                status.value(),
                status.getReasonPhrase(),
                status.name(),
                message,
                message,
                path,
                path,
                validationErrors
        );

        return ResponseEntity
                .status(status)
                .body(body);
    }
}

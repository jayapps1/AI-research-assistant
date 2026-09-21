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
                exception.getErrorCode() != null ? exception.getErrorCode() : HttpStatus.UNAUTHORIZED.name(),
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(com.researchassistant.ai.exception.AiDevelopmentBudgetExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleAiDevelopmentBudgetExceeded(
            com.researchassistant.ai.exception.AiDevelopmentBudgetExceededException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.TOO_MANY_REQUESTS,
                "AI_DEVELOPMENT_BUDGET_EXCEEDED",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "budgetLimitUsd", String.valueOf(exception.getBudgetLimitUsd() != null ? exception.getBudgetLimitUsd() : java.math.BigDecimal.ZERO),
                        "currentSpendUsd", String.valueOf(exception.getCurrentSpendUsd() != null ? exception.getCurrentSpendUsd() : java.math.BigDecimal.ZERO)
                )
        );
    }

    @ExceptionHandler(com.researchassistant.billing.exception.InvalidPaymentReferenceException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPaymentReference(
            com.researchassistant.billing.exception.InvalidPaymentReferenceException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                exception.getErrorCode() != null ? exception.getErrorCode() : "INVALID_PAYMENT_REFERENCE",
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

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleHttpMessageNotReadable(
            org.springframework.http.converter.HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        logException("INVALID_REQUEST_PAYLOAD", exception, request);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "INVALID_REQUEST_PAYLOAD",
                "Invalid request payload or malformed field values.",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(RagCapabilityUnavailableException.class)
    public ResponseEntity<ApiErrorResponse> handleRagCapabilityUnavailable(
            RagCapabilityUnavailableException exception,
            HttpServletRequest request
    ) {
        logException("AI_PROVIDER_NOT_CONFIGURED", exception, request);
        return buildResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "AI_PROVIDER_NOT_CONFIGURED",
                "AI service is currently unavailable or disabled.",
                request.getRequestURI(),
                Map.of()
        );
    }

    @ExceptionHandler(com.researchassistant.ai.exception.AiGenerationException.class)
    public ResponseEntity<ApiErrorResponse> handleAiGeneration(
            com.researchassistant.ai.exception.AiGenerationException exception,
            HttpServletRequest request
    ) {
        logException(exception.getCode(), exception, request);
        HttpStatus status = switch (exception.getCode()) {
            case "AI_PROVIDER_NOT_CONFIGURED", "AI_PROVIDER_AUTHENTICATION_FAILED", "AI_PROVIDER_UNAVAILABLE" -> HttpStatus.SERVICE_UNAVAILABLE;
            case "AI_PROVIDER_ACCESS_DENIED" -> HttpStatus.FORBIDDEN;
            case "AI_MODEL_UNAVAILABLE" -> HttpStatus.BAD_GATEWAY;
            case "AI_PROVIDER_RATE_LIMITED", "AI_PROVIDER_QUOTA_EXHAUSTED", "AI_PROVIDER_BILLING_UNAVAILABLE" -> HttpStatus.TOO_MANY_REQUESTS;
            case "AI_PROVIDER_TIMEOUT" -> HttpStatus.GATEWAY_TIMEOUT;
            case "AI_PROVIDER_REQUEST_INVALID" -> HttpStatus.BAD_REQUEST;
            case "AI_CREDITS_EXHAUSTED" -> HttpStatus.PAYMENT_REQUIRED;
            case "AI_INSUFFICIENT_EVIDENCE", "AI_CITATION_VERIFICATION_FAILED" -> HttpStatus.UNPROCESSABLE_ENTITY;
            case "AI_RESPONSE_INVALID" -> HttpStatus.BAD_GATEWAY;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
        return buildResponse(
                status,
                exception.getCode(),
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
        logException("AI_CITATION_VERIFICATION_FAILED", exception, request);
        return buildResponse(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "AI_CITATION_VERIFICATION_FAILED",
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

    @ExceptionHandler(com.researchassistant.billing.aicredit.exception.AiCreditsExhaustedException.class)
    public ResponseEntity<ApiErrorResponse> handleAiCreditsExhausted(
            com.researchassistant.billing.aicredit.exception.AiCreditsExhaustedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.PAYMENT_REQUIRED,
                "AI_CREDITS_EXHAUSTED",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of(
                        "workspaceId", exception.getWorkspaceId() != null ? exception.getWorkspaceId().toString() : "",
                        "includedRemaining", exception.getIncludedRemaining().toPlainString(),
                        "promotionalRemaining", exception.getPromotionalRemaining().toPlainString(),
                        "purchasedRemaining", exception.getPurchasedRemaining().toPlainString(),
                        "totalAvailable", exception.getTotalAvailable().toPlainString(),
                        "canPurchaseCredits", String.valueOf(exception.isCanPurchaseCredits())
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
        String msg = exception.getMessage() != null ? exception.getMessage() : "";
        if (msg.toLowerCase().contains("paystack") && (msg.toLowerCase().contains("secret key") || msg.toLowerCase().contains("not configured") || msg.toLowerCase().contains("disabled"))) {
            logException("PAYMENT_PROVIDER_NOT_CONFIGURED", exception, request);
            return buildResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "PAYMENT_PROVIDER_NOT_CONFIGURED",
                    "Paystack payment gateway is not configured for this environment.",
                    request.getRequestURI(),
                    Map.of()
            );
        }
        if (msg.toLowerCase().contains("openai") && (msg.toLowerCase().contains("disabled") || msg.toLowerCase().contains("unavailable"))) {
            logException("AI_PROVIDER_NOT_CONFIGURED", exception, request);
            return buildResponse(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI_PROVIDER_NOT_CONFIGURED",
                    "AI service is currently unavailable or disabled.",
                    request.getRequestURI(),
                    Map.of()
            );
        }
        logException("ILLEGAL_STATE", exception, request);
        return buildResponse(
                HttpStatus.CONFLICT,
                "ILLEGAL_STATE",
                exception.getMessage(),
                request.getRequestURI(),
                Map.of()
        );
    }


    @ExceptionHandler(com.researchassistant.identity.exception.InvalidPhoneNumberException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidPhoneNumber(
            com.researchassistant.identity.exception.InvalidPhoneNumberException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request.getRequestURI(),
                Map.of("phoneNumber", exception.getMessage())
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
    public ResponseEntity<ApiErrorResponse> handleValidationErrors(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {

        Map<String, String> errors = new LinkedHashMap<>();

        exception
                .getBindingResult()
                .getFieldErrors()
                .forEach(error -> errors.put(
                        error.getField(),
                        error.getDefaultMessage()
                ));

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Request validation failed.",
                request.getRequestURI(),
                errors
        );
    }


    /**
     * Fallback handler for unhandled runtime exceptions.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneralException(
            Exception exception,
            HttpServletRequest request
    ) {
        logException("INTERNAL_SERVER_ERROR", exception, request);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_SERVER_ERROR",
                "An unexpected internal error occurred.",
                request.getRequestURI(),
                Map.of()
        );
    }

    private void logException(String code, Throwable t, HttpServletRequest request) {
        String correlationId = MDC.get("requestId");
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error(
                "API Error [code={}, correlationId={}, uri={}]: {}",
                code, correlationId, request.getRequestURI(), t.getMessage(), t
        );
    }


    /**
     * Builds the standard API error response with custom error code.
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String errorCode,
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
                errorCode != null ? errorCode : status.name(),
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

    /**
     * Builds the standard API error response.
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            String path,
            Map<String, String> validationErrors
    ) {
        return buildResponse(status, status.name(), message, path, validationErrors);
    }
}

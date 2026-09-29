package com.researchassistant.common.storage;

public class StorageException extends RuntimeException {

    private final String errorCode;

    public StorageException(String message) {
        this("STORAGE_OPERATION_FAILED", message);
    }

    public StorageException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public StorageException(String message, Throwable cause) {
        this("STORAGE_OPERATION_FAILED", message, cause);
    }

    public StorageException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

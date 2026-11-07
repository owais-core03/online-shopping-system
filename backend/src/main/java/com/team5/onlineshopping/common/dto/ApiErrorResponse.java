package com.team5.onlineshopping.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {

    private boolean success;
    private String errorCode;
    private String message;
    private List<String> details;
    private Instant timestamp;

    public ApiErrorResponse() {
        this.success = false;
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(String errorCode, String message) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.timestamp = Instant.now();
    }

    public ApiErrorResponse(String errorCode, String message, List<String> details) {
        this.success = false;
        this.errorCode = errorCode;
        this.message = message;
        this.details = details;
        this.timestamp = Instant.now();
    }

    public static ApiErrorResponse of(String errorCode, String message) {
        return new ApiErrorResponse(errorCode, message);
    }

    public static ApiErrorResponse of(String errorCode, String message, List<String> details) {
        return new ApiErrorResponse(errorCode, message, details);
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<String> getDetails() {
        return details;
    }

    public void setDetails(List<String> details) {
        this.details = details;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}

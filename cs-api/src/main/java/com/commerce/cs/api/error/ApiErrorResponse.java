package com.commerce.cs.api.error;

import java.util.List;

public record ApiErrorResponse(
    String code,
    String message,
    List<FieldErrorResponse> errors
) {

    public ApiErrorResponse {
        errors = errors == null ? List.of() : List.copyOf(errors);
    }

    public static ApiErrorResponse of(String code, String message) {
        return new ApiErrorResponse(code, message, List.of());
    }

    public static ApiErrorResponse validation(List<FieldErrorResponse> errors) {
        return new ApiErrorResponse("VALIDATION_ERROR", "Request validation failed.", errors);
    }
}

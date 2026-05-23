package com.commerce.cs.api.error;

public record FieldErrorResponse(
    String field,
    String message
) {
}

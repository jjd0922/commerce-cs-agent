package com.commerce.cs.application.tool;

public sealed interface ValidationResult permits ValidationResult.Ok, ValidationResult.RequiresAuthentication, ValidationResult.RequiresConfirmation {

    default boolean valid() {
        return this instanceof Ok;
    }

    record Ok() implements ValidationResult {
    }

    record RequiresAuthentication(String message) implements ValidationResult {
    }

    record RequiresConfirmation(String message) implements ValidationResult {
    }
}

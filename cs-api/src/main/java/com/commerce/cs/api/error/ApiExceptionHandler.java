package com.commerce.cs.api.error;

import com.commerce.cs.application.tool.UnknownToolException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Comparator;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorResponse> errors = exception.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(error -> new FieldErrorResponse(error.getField(), error.getDefaultMessage()))
            .sorted(Comparator.comparing(FieldErrorResponse::field))
            .toList();
        return ResponseEntity.badRequest().body(ApiErrorResponse.validation(errors));
    }

    @ExceptionHandler(UnknownToolException.class)
    public ResponseEntity<ApiErrorResponse> handleUnknownTool(UnknownToolException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ApiErrorResponse.of("UNKNOWN_TOOL", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
            .body(ApiErrorResponse.of("BAD_REQUEST", exception.getMessage()));
    }
}

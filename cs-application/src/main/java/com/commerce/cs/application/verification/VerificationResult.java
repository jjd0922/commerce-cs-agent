package com.commerce.cs.application.verification;

public sealed interface VerificationResult permits VerificationResult.Success, VerificationResult.Failure {

    record Success(String userId) implements VerificationResult {
    }

    record Failure(String reason) implements VerificationResult {
    }
}

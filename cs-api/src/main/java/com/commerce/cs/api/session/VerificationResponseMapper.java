package com.commerce.cs.api.session;

import com.commerce.cs.application.verification.VerificationResult;

public final class VerificationResponseMapper {

    private VerificationResponseMapper() {
    }

    public static VerificationResponse from(VerificationResult result) {
        if (result instanceof VerificationResult.Success success) {
            return VerificationResponse.success(success.userId());
        }
        if (result instanceof VerificationResult.Failure failure) {
            return VerificationResponse.failure(failure.reason());
        }
        throw new IllegalArgumentException("Unsupported verification result: " + result.getClass().getName());
    }
}

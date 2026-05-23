package com.commerce.cs.application.verification;

public interface VerificationUseCase {

    VerificationResult verify(VerificationCommand command);
}

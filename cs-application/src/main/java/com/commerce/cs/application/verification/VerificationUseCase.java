package com.commerce.cs.application.verification;

import com.commerce.cs.application.session.SessionState;
import com.commerce.cs.application.session.SessionStore;

import java.time.Duration;

public final class VerificationUseCase {

    private static final Duration AUTHENTICATED_SESSION_TTL = Duration.ofMinutes(30);

    private final CustomerLookupPort customerLookupPort;
    private final SessionStore sessionStore;

    public VerificationUseCase(CustomerLookupPort customerLookupPort, SessionStore sessionStore) {
        this.customerLookupPort = customerLookupPort;
        this.sessionStore = sessionStore;
    }

    public VerificationResult verify(VerificationCommand command) {
        return customerLookupPort.findByEmail(command.email())
            .filter(customer -> customer.matches(command.email(), command.phoneLast4()))
            .<VerificationResult>map(customer -> {
                sessionStore.save(
                    new SessionState(command.sessionId(), customer.userId(), true),
                    AUTHENTICATED_SESSION_TTL
                );
                return new VerificationResult.Success(customer.userId());
            })
            .orElseGet(() -> new VerificationResult.Failure("Customer verification failed."));
    }
}

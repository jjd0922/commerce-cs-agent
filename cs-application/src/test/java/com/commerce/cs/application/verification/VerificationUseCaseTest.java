package com.commerce.cs.application.verification;

import com.commerce.cs.application.session.SessionState;
import com.commerce.cs.application.session.SessionStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationUseCaseTest {

    @Test
    void verification_succeeds_when_email_and_phone_last4_match() {
        FakeCustomerLookupPort customerLookupPort = new FakeCustomerLookupPort();
        customerLookupPort.save(new CustomerIdentity("user-1", "customer@example.com", "5678"));
        FakeSessionStore sessionStore = new FakeSessionStore();
        VerificationUseCase useCase = new VerificationService(customerLookupPort, sessionStore);

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "customer@example.com", "5678")
        );

        assertThat(result).isEqualTo(new VerificationResult.Success("user-1"));
        assertThat(sessionStore.findById("session-1"))
            .hasValueSatisfying(session -> {
                assertThat(session.userId()).isEqualTo("user-1");
                assertThat(session.authenticated()).isTrue();
            });
    }

    @Test
    void verification_fails_when_customer_does_not_exist() {
        VerificationUseCase useCase = new VerificationService(new FakeCustomerLookupPort(), new FakeSessionStore());

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "missing@example.com", "5678")
        );

        assertThat(result).isInstanceOf(VerificationResult.Failure.class);
    }

    @Test
    void verification_fails_when_phone_last4_does_not_match() {
        FakeCustomerLookupPort customerLookupPort = new FakeCustomerLookupPort();
        customerLookupPort.save(new CustomerIdentity("user-1", "customer@example.com", "5678"));
        FakeSessionStore sessionStore = new FakeSessionStore();
        VerificationUseCase useCase = new VerificationService(customerLookupPort, sessionStore);

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "customer@example.com", "0000")
        );

        assertThat(result).isInstanceOf(VerificationResult.Failure.class);
        assertThat(sessionStore.findById("session-1")).isEmpty();
    }

    private static final class FakeCustomerLookupPort implements CustomerLookupPort {

        private final Map<String, CustomerIdentity> customers = new HashMap<>();

        private void save(CustomerIdentity customerIdentity) {
            customers.put(customerIdentity.email().toLowerCase(), customerIdentity);
        }

        @Override
        public Optional<CustomerIdentity> findByEmail(String email) {
            return Optional.ofNullable(customers.get(email.toLowerCase()));
        }
    }

    private static final class FakeSessionStore implements SessionStore {

        private final Map<String, SessionState> sessions = new HashMap<>();

        @Override
        public Optional<SessionState> findById(String sessionId) {
            return Optional.ofNullable(sessions.get(sessionId));
        }

        @Override
        public void save(SessionState sessionState, Duration ttl) {
            sessions.put(sessionState.sessionId(), sessionState);
        }
    }
}

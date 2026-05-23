package com.commerce.cs.application.verification;

import com.commerce.cs.application.session.SessionState;
import com.commerce.cs.application.session.SessionStore;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("VerificationService 본인확인 흐름")
@ExtendWith(MockitoExtension.class)
class VerificationUseCaseTest {

    @Mock
    private CustomerLookupPort customerLookupPort;

    @Mock
    private SessionStore sessionStore;

    @Test
    @DisplayName("이메일과 휴대폰 뒤 4자리가 일치하면 인증 세션을 저장하고 성공을 반환한다")
    void verification_succeeds_when_email_and_phone_last4_match() {
        CustomerIdentity customer = new CustomerIdentity("user-1", "customer@example.com", "5678");
        when(customerLookupPort.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        VerificationUseCase useCase = new VerificationService(customerLookupPort, sessionStore);

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "customer@example.com", "5678")
        );

        assertThat(result).isEqualTo(new VerificationResult.Success("user-1"));
        verify(sessionStore).save(
            argThat(session ->
                session.sessionId().equals("session-1")
                    && session.userId().equals("user-1")
                    && session.authenticated()
            ),
            eq(Duration.ofMinutes(30))
        );
    }

    @Test
    @DisplayName("고객 이메일을 찾지 못하면 실패를 반환하고 세션을 저장하지 않는다")
    void verification_fails_when_customer_does_not_exist() {
        when(customerLookupPort.findByEmail("missing@example.com")).thenReturn(Optional.empty());
        VerificationUseCase useCase = new VerificationService(customerLookupPort, sessionStore);

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "missing@example.com", "5678")
        );

        assertThat(result).isInstanceOf(VerificationResult.Failure.class);
        verify(sessionStore, never()).save(
            any(SessionState.class),
            any(Duration.class)
        );
    }

    @Test
    @DisplayName("휴대폰 뒤 4자리가 일치하지 않으면 실패를 반환하고 세션을 저장하지 않는다")
    void verification_fails_when_phone_last4_does_not_match() {
        CustomerIdentity customer = new CustomerIdentity("user-1", "customer@example.com", "5678");
        when(customerLookupPort.findByEmail("customer@example.com")).thenReturn(Optional.of(customer));
        VerificationUseCase useCase = new VerificationService(customerLookupPort, sessionStore);

        VerificationResult result = useCase.verify(
            new VerificationCommand("session-1", "customer@example.com", "0000")
        );

        assertThat(result).isInstanceOf(VerificationResult.Failure.class);
        verify(sessionStore, never()).save(
            any(SessionState.class),
            any(Duration.class)
        );
    }
}

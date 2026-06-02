package com.commerce.cs.api.session;

import com.commerce.cs.application.verification.VerificationResult;
import com.commerce.cs.application.verification.VerificationUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("SessionController 본인확인 응답 매핑")
@ExtendWith(MockitoExtension.class)
class SessionControllerTest {

    @Mock
    private VerificationUseCase verificationUseCase;

    @Test
    @DisplayName("본인확인 성공 결과를 SUCCESS 응답으로 변환한다")
    void verify_returns_success_response() {
        when(verificationUseCase.verify(argThat(command ->
            command.sessionId().equals("session-1")
                && command.email().equals("customer@example.com")
                && command.phoneLast4().equals("5678")
        ))).thenReturn(new VerificationResult.Success("user-1"));
        SessionController controller = new SessionController(verificationUseCase);

        VerificationResponse response = controller.verify(
            new VerificationRequest("session-1", "customer@example.com", "5678")
        ).getBody();

        assertThat(response).isEqualTo(VerificationResponse.success("user-1"));
        verify(verificationUseCase).verify(argThat(command ->
            command.sessionId().equals("session-1")
                && command.email().equals("customer@example.com")
                && command.phoneLast4().equals("5678")
        ));
    }

    @Test
    @DisplayName("본인확인 실패 결과를 FAILURE 응답으로 변환한다")
    void verify_returns_failure_response() {
        when(verificationUseCase.verify(argThat(command -> command.email().equals("missing@example.com"))))
            .thenReturn(new VerificationResult.Failure("Customer verification failed."));
        SessionController controller = new SessionController(verificationUseCase);

        VerificationResponse response = controller.verify(
            new VerificationRequest("session-1", "missing@example.com", "0000")
        ).getBody();

        assertThat(response).isEqualTo(VerificationResponse.failure("Customer verification failed."));
    }
}

package com.commerce.cs.api.session;

import com.commerce.cs.application.verification.VerificationCommand;
import com.commerce.cs.application.verification.VerificationResult;
import com.commerce.cs.application.verification.VerificationUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final VerificationUseCase verificationUseCase;

    public SessionController(VerificationUseCase verificationUseCase) {
        this.verificationUseCase = verificationUseCase;
    }

    @PostMapping("/verify")
    public ResponseEntity<VerificationResponse> verify(@RequestBody VerificationRequest request) {
        VerificationResult result = verificationUseCase.verify(
            new VerificationCommand(request.sessionId(), request.email(), request.phoneLast4())
        );
        return ResponseEntity.ok(VerificationResponseMapper.from(result));
    }
}

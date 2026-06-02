package com.commerce.cs.api.session;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerificationRequest(
    @NotBlank(message = "sessionId must not be blank")
    String sessionId,

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be valid")
    String email,

    @NotBlank(message = "phoneLast4 must not be blank")
    @Pattern(regexp = "\\d{4}", message = "phoneLast4 must be 4 digits")
    String phoneLast4
) {
}

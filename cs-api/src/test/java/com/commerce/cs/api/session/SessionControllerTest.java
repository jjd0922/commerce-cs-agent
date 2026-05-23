package com.commerce.cs.api.session;

import com.commerce.cs.api.error.ApiExceptionHandler;
import com.commerce.cs.application.verification.VerificationResult;
import com.commerce.cs.application.verification.VerificationUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    @DisplayName("본인확인 요청 검증 실패 시 VALIDATION_ERROR 응답을 반환한다")
    void verify_returns_validation_error_when_request_is_invalid() throws Exception {
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(post("/api/sessions/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sessionId": "",
                      "email": "not-email",
                      "phoneLast4": "12"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors[?(@.field == 'sessionId')]").exists())
            .andExpect(jsonPath("$.errors[?(@.field == 'email')]").exists())
            .andExpect(jsonPath("$.errors[?(@.field == 'phoneLast4')]").exists());
    }

    @Test
    @DisplayName("IllegalArgumentException은 BAD_REQUEST 응답으로 변환한다")
    void verify_maps_illegal_argument_exception_to_error_response() throws Exception {
        when(verificationUseCase.verify(argThat(command -> command.email().equals("customer@example.com"))))
            .thenThrow(new IllegalArgumentException("invalid verification request"));
        MockMvc mockMvc = mockMvc();

        mockMvc.perform(post("/api/sessions/verify")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "sessionId": "session-1",
                      "email": "customer@example.com",
                      "phoneLast4": "5678"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("BAD_REQUEST"))
            .andExpect(jsonPath("$.message").value("invalid verification request"));
    }

    private MockMvc mockMvc() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        return MockMvcBuilders.standaloneSetup(new SessionController(verificationUseCase))
            .setControllerAdvice(new ApiExceptionHandler())
            .setValidator(validator)
            .setMessageConverters(new MappingJackson2HttpMessageConverter(new ObjectMapper()))
            .build();
    }
}

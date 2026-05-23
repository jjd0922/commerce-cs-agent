package com.commerce.cs.application.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PiiMasker 개인정보 마스킹 규칙")
class PiiMaskerTest {

    @Test
    @DisplayName("하이픈이 있는 휴대폰 번호의 가운데 자리를 마스킹한다")
    void masks_phone_number() {
        String masked = PiiMasker.mask("phone=010-1234-5678");

        assertThat(masked).isEqualTo("phone=010-****-5678");
    }

    @Test
    @DisplayName("하이픈이 없는 휴대폰 번호를 표준 형식으로 마스킹한다")
    void masks_phone_number_without_hyphen() {
        String masked = PiiMasker.mask("phone=01012345678");

        assertThat(masked).isEqualTo("phone=010-****-5678");
    }

    @Test
    @DisplayName("이메일 로컬 파트의 중간 문자를 마스킹한다")
    void masks_email_local_part() {
        String masked = PiiMasker.mask("email=customer@example.com");

        assertThat(masked).isEqualTo("email=c******r@example.com");
    }

    @Test
    @DisplayName("주민등록번호는 전화번호보다 먼저 전체 마스킹한다")
    void masks_resident_registration_number_before_phone_number() {
        String masked = PiiMasker.mask("rrn=900101-1234567");

        assertThat(masked).isEqualTo("rrn=******-*******");
    }

    @Test
    @DisplayName("주민등록번호와 휴대폰 번호가 함께 있어도 각각 올바르게 마스킹한다")
    void masks_resident_registration_number_and_phone_number_together() {
        String masked = PiiMasker.mask("rrn=900101-1234567, phone=01012345678");

        assertThat(masked).isEqualTo("rrn=******-*******, phone=010-****-5678");
    }
}

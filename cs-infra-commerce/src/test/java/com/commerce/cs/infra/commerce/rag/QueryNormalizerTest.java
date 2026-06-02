package com.commerce.cs.infra.commerce.rag;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("RAG 쿼리 정규화")
class QueryNormalizerTest {

    @Test
    @DisplayName("대소문자, 앞뒤 공백, 연속 공백을 정규화한다")
    void normalizes_case_and_spaces() {
        String normalized = QueryNormalizer.normalize("  Return   POLICY  ");

        assertThat(normalized).isEqualTo("return policy");
    }

    @Test
    @DisplayName("검색 키에 불필요한 특수문자를 제거한다")
    void removes_symbols() {
        String normalized = QueryNormalizer.normalize("return-policy!!!");

        assertThat(normalized).isEqualTo("returnpolicy");
    }
}

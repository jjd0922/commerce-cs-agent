# Mock LLM 평가 실행 결과

## 실행 환경

| 항목 | 값 |
|---|---|
| 실행일 | 2026-05-28 |
| 브랜치 | `PR/21-evaluation-harness` |
| Java | 17 |
| 평가 profile | `demo` |
| LLM gateway | Mocked `AnthropicGateway` |
| 통합 테스트 backend | Testcontainers MySQL 8.4 |

## 요약

이 문서는 실제 LLM 품질 평가가 아니라, mocked LLM gateway를 사용한 결정적 백엔드 제어 흐름 평가 결과다.

| 항목 | 결과 |
|---|---:|
| 평가 케이스 | 9 |
| 평가 JUnit 테스트 | 10 |
| 평가 실패 | 0 |
| 평가 에러 | 0 |
| 평가 skip | 0 |
| 평가 suite 시간 | 2.170s |
| 일반 회귀 테스트 | 101 passed |
| 통합 테스트 | 3 passed |

## 실행 명령

```powershell
.\gradlew.bat :cs-bootstrap:test --tests com.commerce.cs.bootstrap.ChatEvaluationRunnerTest
.\gradlew.bat test
.\gradlew.bat :cs-bootstrap:integrationTest
```

세 명령 모두 `BUILD SUCCESSFUL`로 완료되었다.

## 케이스별 결과

| 케이스 | 기대 응답 | 기대 도구 | 기대 side effect | 시간 | 결과 |
|---|---|---|---|---:|---|
| `faq-search` | `TOOL_EXECUTED` | `search_faq` | 없음 | 0.409s | PASS |
| `policy-search` | `TOOL_EXECUTED` | `get_policy` | 없음 | 0.010s | PASS |
| `product-search` | `TOOL_EXECUTED` | `search_product` | 없음 | 0.009s | PASS |
| `auth-required` | `REQUIRES_AUTHENTICATION` | `request_return` | 없음 | 0.009s | PASS |
| `return-confirmation` | `REQUIRES_CONFIRMATION` | `request_return` | pending action 저장 | 0.068s | PASS |
| `return-execution` | `TOOL_EXECUTED` | `request_return` | return + outbox event 생성 | 0.054s | PASS |
| `return-policy-violation` | `BAD_REQUEST` | `request_return` | return/outbox 없음, pending action 유지 | 0.038s | PASS |
| `idempotency` | `TOOL_EXECUTED` | `request_return` | return 1건 + outbox event 1건만 생성 | 0.037s | PASS |
| `llm-fallback` | `TEXT` | 없음 | 없음 | 0.010s | PASS |

## 통합 테스트 결과

| 테스트 클래스 | 테스트 수 | 실패 | 에러 | Skip | 시간 | 결과 |
|---|---:|---:|---:|---:|---:|---|
| `LocalDatabaseProfileContextTest` | 2 | 0 | 0 | 0 | 0.651s | PASS |
| `OutboxPollerConcurrencyTest` | 1 | 0 | 0 | 0 | 0.713s | PASS |

## 검증된 내용

- 평가 카탈로그가 핵심 시나리오를 포함한다.
- LLM tool use 이후의 백엔드 제어 흐름이 기대 응답 타입을 반환한다.
- 변경성 도구는 인증과 confirmation 게이트를 통과해야 실행된다.
- 확인된 반품 요청은 정확히 하나의 return과 하나의 outbox event를 생성한다.
- 반복 확인 요청은 중복 side effect를 만들지 않는다.
- 도메인 정책 위반은 `BAD_REQUEST`로 노출된다.
- local persistence profile과 outbox 동시 polling 경로가 Testcontainers MySQL 환경에서 동작한다.

## 한계

- 이 결과는 mocked LLM gateway 기반이므로 실제 모델의 tool selection 정확도를 의미하지 않는다.
- FAQ, 정책, 상품 검색의 retrieval 품질은 측정하지 않는다.
- 외부 커머스 시스템은 demo 또는 in-memory adapter로 대체되어 있다.
- 케이스별 시간은 JUnit 실행 시간이며, 운영 환경의 API latency 지표가 아니다.

실제 LLM 기준 측정은 [실제 LLM 평가 결과](live-llm-evaluation-result.md)를 별도로 참조한다.

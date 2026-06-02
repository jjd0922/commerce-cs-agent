# 설계 개요

## 목표

이 프로젝트의 목표는 단순한 LLM 챗봇이 아니라, LLM이 제안한 의도를 백엔드 도메인 규칙과 안전장치 안에서 검증하고 실행하는 고객지원 에이전트를 구현하는 것이다.

LLM은 사용자 의도 파악과 도구 선택을 돕지만, 최종 정책 판단과 변경성 작업 실행은 백엔드가 담당한다.

## 설계 원칙

- LLM 응답은 신뢰할 수 없는 intent signal로 취급한다.
- 변경성 도구는 인증과 confirmation 없이 실행하지 않는다.
- 도메인 정책 검증은 LLM이 아니라 application/domain 계층에서 수행한다.
- 중복 요청은 idempotency key로 제어한다.
- 동시 실행은 lock port를 통해 제어한다.
- 외부 발행은 transaction과 분리된 outbox로 처리한다.
- runtime adapter는 profile로 분리한다.
- 일반 테스트와 실제 LLM/API 기반 측정은 분리한다.

## 계층 경계

| 계층 | 모듈 | 책임 |
|---|---|---|
| domain | `cs-domain` | 도메인 모델, 상태 전이, 정책, 이벤트 |
| application | `cs-application` | UseCase, port, tool 검증과 실행 흐름 |
| api | `cs-api` | HTTP API, DTO, 예외 응답 |
| infra | `cs-infra-*` | LLM, RAG, persistence adapter |
| bootstrap | `cs-bootstrap` | Spring Boot 실행, profile wiring |
| test | `cs-test-support` | 계층 의존성 검증 |

ArchUnit 테스트는 다음 경계를 검증한다.

- domain은 application, api, infra, bootstrap에 의존하지 않는다.
- application은 api, infra, bootstrap에 의존하지 않는다.
- api는 domain, infra, bootstrap에 의존하지 않는다.

## 핵심 제어 흐름

변경성 tool use는 다음 흐름을 통과한다.

```text
LLM tool_use
-> ToolExecutor.validate()
-> authentication check
-> confirmation check
-> domain policy validation
-> idempotency check
-> distributed lock
-> transaction
-> outbox
```

이 구조 때문에 LLM이 `request_return` 도구를 선택하더라도, 인증되지 않은 세션에서는 실제 반품 요청이 생성되지 않는다.

## Confirmation

변경성 도구는 2단계로 처리한다.

1. 최초 요청에서 `RequiresConfirmation` 응답을 반환하고 pending action을 저장한다.
2. 사용자가 승인하면 pending action의 tool name, args, idempotency key로 다시 검증한 뒤 실행한다.

사용자가 거절하면 pending action은 제거되고 도구는 실행되지 않는다.

## Idempotency

동일 세션, 동일 도구, 동일 인자 조합에서 생성한 idempotency key를 사용한다.
확인 후 실행 단계에서도 pending action에 저장된 key를 재사용하므로, 반복 확인 요청이 중복 side effect를 만들지 않는다.

## Outbox

반품 요청 생성과 외부 이벤트 발행은 분리한다.

- `ReturnService`는 트랜잭션 안에서 return과 outbox event를 저장한다.
- `OutboxPoller`는 pending outbox message를 조회해 발행한다.
- 발행 실패는 retry count와 failure reason으로 추적한다.
- retry 한도를 넘기면 dead letter 상태로 전이한다.

## Profile

| Profile | 목적 |
|---|---|
| `demo` | in-memory adapter 기반 로컬 데모와 mock 평가 |
| `local` | MySQL/Flyway/JPA 기반 통합 테스트와 로컬 DB 실행 |
| `live-llm` | 실제 Anthropic API 호출 기반 LLM 평가 |

실제 LLM 평가는 일반 `test`에 포함하지 않고 `liveLlmEval` 태스크로 분리한다.

## 평가 전략

평가는 두 종류로 분리한다.

| 평가 | 목적 |
|---|---|
| mock LLM 평가 | 백엔드 제어 흐름의 결정적 회귀 검증 |
| live LLM 평가 | 실제 모델의 tool selection 측정 |

mock LLM 평가는 일반 회귀 테스트에 포함하기 적합하고, CI를 도입할 경우에도 비용 없이 실행할 수 있다.
live LLM 평가는 API key, 네트워크, 비용, 모델 비결정성이 있으므로 별도 태스크에서 실행한다.

## 포트폴리오 관점의 핵심 포인트

이 프로젝트에서 강조할 지점은 LLM 자체보다 백엔드 안전장치다.

- LLM이 변경성 작업을 직접 실행하지 못한다.
- 인증, confirmation, 정책 검증, 멱등성, 락, 트랜잭션, outbox가 분리되어 있다.
- 테스트는 단위, API, E2E, 통합, ArchUnit, 평가셋으로 나뉜다.
- 실제 LLM 평가는 비용과 비결정성을 고려해 별도 측정 경로로 분리되어 있다.

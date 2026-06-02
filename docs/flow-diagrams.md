# 시스템 플로우 다이어그램

이 문서는 LLM 기반 고객지원 에이전트의 주요 실행 경계와 평가 흐름을 시각화한다.
결과 수치는 별도 결과 문서에서 관리하고, 이 문서는 설계 의도와 제어 흐름을 빠르게 이해하기 위한 용도다.

## 전체 아키텍처

```mermaid
flowchart LR
    User[사용자] --> Api[cs-api<br/>Chat API]
    Api --> App[cs-application<br/>ChatService / ToolExecutor]
    App --> LlmPort[LLM Port<br/>AnthropicGateway]
    LlmPort --> LlmAdapter[cs-infra-llm<br/>Demo 또는 Anthropic API]
    App --> Domain[cs-domain<br/>정책 / 상태 전이 / 이벤트]
    App --> RagPort[RAG Port]
    RagPort --> RagAdapter[cs-infra-rag<br/>FAQ / 정책 / 상품 검색]
    App --> Persistence[cs-infra-persistence<br/>Return / PendingAction / Outbox]
    Persistence --> Db[(MySQL 또는 In-memory)]
```

핵심 경계는 LLM을 실행 주체가 아니라 의도 신호 제공자로 제한하는 것이다.
도구 실행, 인증, 확인, 정책 검증, 멱등성, 트랜잭션 처리는 application과 domain 계층에서 수행한다.

## 변경성 도구 실행 흐름

```mermaid
flowchart TD
    Start[사용자 반품 요청] --> ToolUse[LLM tool_use<br/>request_return]
    ToolUse --> Validate[ToolExecutor.validate]
    Validate --> Auth{인증?}
    Auth -- 아니오 --> RequireAuth[REQUIRES_AUTHENTICATION]
    Auth -- 예 --> Confirm{확인 완료?}
    Confirm -- 아니오 --> Pending[PendingAction 저장]
    Pending --> RequireConfirm[REQUIRES_CONFIRMATION]
    Confirm -- 예 --> Policy{도메인 정책 통과?}
    Policy -- 아니오 --> BadRequest[BAD_REQUEST]
    Policy -- 예 --> Idempotency{이미 처리됨?}
    Idempotency -- 예 --> Existing[기존 결과 반환]
    Idempotency -- 아니오 --> Lock[분산 락]
    Lock --> Tx[트랜잭션]
    Tx --> Return[Return 생성]
    Tx --> Outbox[Outbox event 저장]
    Return --> Done[TOOL_EXECUTED]
    Outbox --> Done
```

이 흐름은 LLM이 변경성 도구를 선택하더라도 실제 side effect가 바로 발생하지 않도록 만든다.
사용자 인증, confirmation, 도메인 정책, 멱등성 검증을 통과한 요청만 반품 생성과 outbox 저장까지 진행한다.

## Outbox 발행 흐름

```mermaid
sequenceDiagram
    participant App as ReturnService
    participant DB as Database
    participant Poller as OutboxPoller
    participant External as External Commerce System

    App->>DB: return 저장
    App->>DB: outbox message 저장
    Poller->>DB: pending message 조회
    Poller->>External: event 발행
    alt 발행 성공
        Poller->>DB: published 처리
    else 발행 실패
        Poller->>DB: retry count / failure reason 기록
    end
```

반품 생성 트랜잭션과 외부 시스템 발행을 분리해 장애 전파 범위를 줄인다.
outbox poller는 실패 사유와 재시도 횟수를 남기며, 동시 polling은 lock 경계에서 제어한다.

## 평가 경로 분리

```mermaid
flowchart LR
    Cases[평가 케이스 YAML] --> MockEval[Mock LLM 평가<br/>ChatEvaluationRunnerTest]
    Cases --> LiveEval[Live LLM 평가<br/>LiveLlmEvaluationRunnerTest]

    MockEval --> MockGateway[Mocked AnthropicGateway]
    MockGateway --> ControlFlow[백엔드 제어 흐름 검증]
    ControlFlow --> UnitReport[JUnit test report]
    ControlFlow --> MockDoc[mock-evaluation-result.md]

    LiveEval --> Anthropic[실제 Anthropic API]
    Anthropic --> ToolMetric[tool selection accuracy 측정]
    ToolMetric --> JsonReport[live-llm-eval-result.json]
    ToolMetric --> LiveDoc[live-llm-evaluation-result.md]
```

mock 평가는 결정적이고 비용이 없어 일반 회귀 테스트에 적합하다.
live LLM 평가는 API key, 네트워크, 모델 비용, 모델 변경 가능성이 있으므로 별도 Gradle task로 분리한다.

## 문서별 역할

| 문서 | 역할 |
|---|---|
| [architecture.md](architecture.md) | 설계 원칙, 계층 경계, 안전장치 설명 |
| [evaluation.md](evaluation.md) | mock 평가 실행 방법과 해석 기준 |
| [mock-evaluation-result.md](mock-evaluation-result.md) | mocked gateway 기반 결정적 평가 결과 |
| [live-llm-evaluation.md](live-llm-evaluation.md) | 실제 LLM 평가 실행 방법과 환경 변수 |
| [live-llm-evaluation-result.md](live-llm-evaluation-result.md) | 실제 모델 기준 측정 결과 |

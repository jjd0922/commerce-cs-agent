# Commerce CS Agent

[![CI](https://github.com/jjd0922/commerce-cs-agent/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/jjd0922/commerce-cs-agent/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange)]()
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.5-green)]()
[![Gradle](https://img.shields.io/badge/Gradle-8.13-blue)]()
[![LLM](https://img.shields.io/badge/LLM-Anthropic-black)]()
[![Evaluation](https://img.shields.io/badge/Evaluation-Mock%20%2B%20Live-brightgreen)]()

LLM이 고객 문의에서 의도를 파악하고 도구를 선택하되, 실제 변경성 작업은 백엔드의 인증, 확인, 도메인 정책, 멱등성, 락, 트랜잭션, 아웃박스 경계를 통과해야 실행되는 커머스 고객지원 에이전트입니다.

이 프로젝트는 LLM을 곧바로 신뢰하지 않고, 백엔드 제어 흐름 안에서 안전하게 사용하는 구조를 보여주는 데 초점을 둡니다.

## 핵심 기능

- 채팅 API와 본인 인증 API
- LLM tool use 기반 FAQ, 정책, 상품 검색
- 반품 요청 도구와 confirmation 흐름
- 인증이 필요한 변경성 도구 차단
- 멱등성 key 기반 중복 실행 방지
- 분산 락 추상화
- 트랜잭션 경계와 outbox 저장
- LLM fallback, retry, circuit breaker
- mock LLM 평가셋과 실제 LLM 평가 경로
- Docker/MySQL 기반 통합 테스트

## 모듈 구조

| 모듈 | 역할 |
|---|---|
| `cs-domain` | 주문, 반품, 금액, 정책, 도메인 이벤트 |
| `cs-application` | UseCase, port, tool execution, session, idempotency |
| `cs-api` | REST controller, request/response DTO, 예외 응답 |
| `cs-infra-commerce` | RAG 검색 도구, 반품 도구 adapter |
| `cs-infra-llm` | Anthropic LLM client, response parser, fallback |
| `cs-infra-persistence` | JPA adapter, Flyway schema, outbox poller |
| `cs-bootstrap` | Spring Boot runtime, profile wiring, 평가 runner |
| `cs-test-support` | ArchUnit 기반 계층 의존성 테스트 |

## 실행

일반 테스트:

```powershell
.\gradlew.bat test
```

Docker/MySQL 기반 통합 테스트:

```powershell
.\gradlew.bat :cs-bootstrap:integrationTest
```

mock LLM 기반 결정적 평가:

```powershell
.\gradlew.bat :cs-bootstrap:test --tests com.commerce.cs.bootstrap.ChatEvaluationRunnerTest
```

실제 LLM 평가:

```powershell
$env:ANTHROPIC_API_KEY="새로_발급한_API_KEY"
$env:ANTHROPIC_MODEL="claude-haiku-4-5-20251001"
.\gradlew.bat :cs-bootstrap:liveLlmEval
```

## 문서

- [설계 개요](docs/architecture.md)
- [시스템 플로우 다이어그램](docs/flow-diagrams.md)
- [평가 실행 가이드](docs/evaluation.md)
- [mock LLM 평가 결과](docs/mock-evaluation-result.md)
- [실제 LLM 평가 가이드](docs/live-llm-evaluation.md)
- [실제 LLM 평가 결과](docs/live-llm-evaluation-result.md)

## 최근 측정 결과

아래 수치는 문서화된 로컬 실행 결과 기준이다. 실행 환경, 모델, 평가 케이스가 바뀌면 다시 측정해야 한다.

mock LLM 기반 결정적 평가:

- 평가 케이스 9개
- 평가 JUnit 테스트 10개
- 실패 0, 에러 0
- 일반 회귀 테스트 101개 통과
- 통합 테스트 3개 통과

실제 LLM 평가:

- 모델: `claude-haiku-4-5-20251001`
- 케이스 수: 5
- tool selection accuracy: 80.0%
- response type accuracy: 100.0%
- unsafe mutation count: 0

## 보안 주의

API key는 코드, 문서, 커밋에 저장하지 않습니다.
실제 LLM 평가는 로컬 환경변수 또는 IntelliJ Run Configuration의 environment variables에만 key를 넣고 실행합니다.

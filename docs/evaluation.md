# 평가 실행 가이드

## 목적

이 평가는 LLM이 도구를 선택한 이후의 백엔드 제어 흐름이 결정적으로 동작하는지 검증한다.
실제 LLM의 답변 품질이나 자연어 이해 성능을 측정하는 평가는 아니다.

핵심 설계 전제는 LLM 응답을 신뢰할 수 없는 의도 신호로 취급한다는 점이다. 변경성 요청은 실제 side effect가 만들어지기 전에 백엔드에서 제어하는 인증, 확인, 도메인 정책 검증, 멱등성, 락, 트랜잭션, 아웃박스 경계를 통과해야 한다.

## 평가 범위

평가셋은 다음 흐름을 검증한다.

- FAQ, 정책, 상품 검색 도구 실행
- 변경성 도구에 대한 인증 게이트
- 변경성 도구에 대한 확인 게이트
- 반품 요청 side effect
- 도메인 정책 위반 거절
- 반복 확인 요청에 대한 멱등성
- 결정적인 fallback 텍스트 응답

평가셋은 다음 항목을 다루지 않는다.

- 실제 LLM의 자연어 이해 품질
- 벡터 DB 검색 품질
- 실제 커머스 외부 시스템 연동 품질
- 동시 요청 상황의 부하, 처리량, 지연 시간

## 평가 케이스

평가 케이스 원본은 다음 파일이다.

```text
cs-bootstrap/src/test/resources/eval/chat-eval-cases.yml
```

각 케이스는 다음 값을 선언한다.

- `messages`: `/api/chat`으로 보낼 사용자 메시지
- `expectedResponseType`: 최종 응답 타입
- `expectedTool`: mocked LLM gateway가 선택해야 하는 도구
- `expectedSideEffects`: return/outbox/pending action 상태 변화

## 실행 명령

결정적 평가셋 실행:

```powershell
.\gradlew.bat :cs-bootstrap:test --tests com.commerce.cs.bootstrap.ChatEvaluationRunnerTest
```

일반 회귀 테스트 실행:

```powershell
.\gradlew.bat test
```

Docker/MySQL 기반 통합 테스트 실행:

```powershell
.\gradlew.bat :cs-bootstrap:integrationTest
```

## IntelliJ 실행

일반 테스트는 테스트 클래스의 gutter 실행 버튼이나 Gradle `test` task로 실행할 수 있다.

통합 테스트는 일반 `test` task에서 제외되어 있다. IntelliJ에서는 Gradle run configuration을 만들고 다음 task를 지정한다.

```text
Tasks: :cs-bootstrap:integrationTest
```

특정 통합 테스트 클래스만 실행하려면 다음처럼 지정한다.

```text
Tasks: :cs-bootstrap:integrationTest --tests com.commerce.cs.bootstrap.LocalDatabaseProfileContextTest
```

## 결과 파일 위치

주요 결과 파일은 다음 위치에 생성된다.

```text
cs-bootstrap/build/test-results/test/TEST-com.commerce.cs.bootstrap.ChatEvaluationRunnerTest.xml
cs-bootstrap/build/test-results/integrationTest/
```

평가 러너는 JUnit 테스트 10개를 보고한다. 이 중 9개는 평가 케이스이고, 1개는 평가 카탈로그 무결성 검증 테스트다.

## 결과 해석

평가가 통과했다는 것은 mocked LLM gateway를 사용한 결정적 환경에서 나열된 시나리오의 백엔드 제어 흐름이 기대 결과와 일치했다는 의미다.

실제 LLM이 항상 올바른 도구를 선택하거나 최적의 답변을 생성한다는 의미는 아니다. 실제 LLM 품질은 모델 호출, 프롬프트, 검색 품질 지표를 포함한 별도 평가로 측정해야 한다.

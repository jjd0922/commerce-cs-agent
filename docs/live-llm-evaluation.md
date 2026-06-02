# 실제 LLM 평가 가이드

## 목적

실제 LLM 평가는 mocked LLM 기반 회귀 테스트와 분리한다.
이 평가는 Claude API를 호출해서 사용자 메시지에 대해 올바른 도구를 선택하는지 측정한다.

## 비용과 보안

- ChatGPT/Claude 웹 구독과 API 과금은 별도다.
- API key는 코드, 문서, 커밋에 저장하지 않는다.
- API key가 노출되면 Anthropic Console에서 폐기하고 새 key를 발급한다.
- 기본 모델은 비용 절감을 위해 `claude-haiku-4-5-20251001`을 사용한다.
- 기본 `max_tokens`는 128이다.

## PowerShell 실행

실행할 PowerShell 세션에서만 환경변수를 설정한다.

```powershell
$env:ANTHROPIC_API_KEY="새로_발급한_API_KEY"
$env:ANTHROPIC_MODEL="claude-haiku-4-5-20251001"
```

실제 LLM 평가 실행:

```powershell
.\gradlew.bat :cs-bootstrap:liveLlmEval
```

`ANTHROPIC_API_KEY`가 없으면 live LLM 평가는 실행되지 않는다.
일반 `test`에서는 `live-llm` 태그가 제외된다.

## IntelliJ 실행

Gradle Run Configuration을 만든다.

```text
Tasks: :cs-bootstrap:liveLlmEval
Environment variables:
ANTHROPIC_API_KEY=새로_발급한_API_KEY
ANTHROPIC_MODEL=claude-haiku-4-5-20251001
```

API key는 Run Configuration의 environment variables에만 넣고 파일에는 저장하지 않는다.

## 평가 케이스

케이스 파일:

```text
cs-bootstrap/src/test/resources/eval/live-llm-eval-cases.yml
```

초기 케이스 수는 비용을 줄이기 위해 5개로 제한한다.

## 결과 파일

실행 후 다음 파일이 생성된다.

```text
cs-bootstrap/build/reports/eval/live-llm-eval-result.json
cs-bootstrap/build/reports/eval/live-llm-eval-summary.md
```

주요 지표:

- tool selection accuracy
- response type accuracy
- error count
- unsafe mutation count
- average latency
- max latency

## 해석 기준

이 평가는 실제 LLM의 도구 선택 정확도를 보기 위한 측정이다.
빌드 통과 여부보다 생성된 리포트의 accuracy와 실패 케이스를 확인하는 것이 중요하다.

특히 `unsafe mutation count`는 0이어야 한다.
반품 같은 변경성 요청은 실제 실행 전에 인증과 확인 게이트에 걸려야 한다.

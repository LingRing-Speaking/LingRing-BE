# call-transcript-processor

LingRing 통화 분석 파이프라인의 AWS Lambda 함수. 통화 녹음 m4a 파일 2개를 받아 Gemini 2.5 Flash 단일 호출로 transcript와 사용자별 피드백을 동시에 생성한 뒤 결과를 SQS에 publish한다.

## 흐름

```
Spring (invoke: EVENT)
   │
   ▼
Lambda
   ├─ S3 GET ×2 (병렬, recordingKey)
   ├─ Gemini 2.5 Flash 1회 호출
   │   · 두 오디오 파일 + 화자 매핑 + JSON schema
   │   → transcript (시간순 segments) + 화자별 analyses
   ├─ userId 일치 검증
   └─ SQS publish (stt-result-queue)
```

이전 OpenAI STT → Chat 두 단계 흐름을 단일 호출로 통합. Lambda 입력 형식은 그대로 유지하고 출력 메시지에 `transcript`와 `analyses`를 함께 담는다.

## 입력 페이로드 (Spring → Lambda)

```json
{
  "callId": 1,
  "recordings": [
    {"userId": 101, "recordingKey": "call-recordings/1/101/<uuid>"},
    {"userId": 202, "recordingKey": "call-recordings/1/202/<uuid>"}
  ]
}
```

> 필드명 `recordingKey`는 Spring 측 `CallRecording.recordingKey` 컨벤션과 일치한다.

## 출력 메시지 (Lambda → SQS `stt-result-queue`)

```json
{
  "callId": 1,
  "modelIdentifier": "gemini-2.5-flash",
  "transcript": {
    "segments": [
      {"userId": 101, "startSec": 0.0, "endSec": 2.1, "text": "Hi"},
      {"userId": 202, "startSec": 2.5, "endSec": 4.8, "text": "Hello"}
    ]
  },
  "analyses": [
    {
      "userId": 101,
      "mistakes": [
        {
          "tag": "GRAMMAR",
          "wrong": "I goed there yesterday",
          "improved": "I went there yesterday",
          "reason": "과거형 동사 사용 오류",
          "koMeaning": "어제 거기 갔어"
        }
      ],
      "positives": [
        {
          "sentence": "That sounds great",
          "goodPart": "자연스러운 동의 표현",
          "koMeaning": "좋네요"
        }
      ]
    },
    {
      "userId": 202,
      "mistakes": [],
      "positives": []
    }
  ]
}
```

- `analyses`는 항상 두 사용자 각각 1개씩 정확히 2개.
- `mistakes[].tag`는 `GRAMMAR`, `VOCABULARY`, `COLLOCATION`, `TONE`, `OTHER` 중 하나.
- `mistakes[].reason`, `mistakes[].koMeaning`, `positives[].goodPart`, `positives[].koMeaning`은 한국어.

## 환경 변수

| 키 | 값 예시 | 설명 |
|---|---|---|
| `STT_RESULT_QUEUE_URL` | `https://sqs.ap-northeast-2.amazonaws.com/<account>/stt-result-queue` | 결과 publish 대상 SQS URL |
| `GEMINI_API_KEY_PARAMETER_NAME` | `/lingring/gemini/api-key` | Gemini API Key를 보관한 SSM Parameter 경로 |
| `RECORDING_BUCKET` | `lingring-dev` | 녹음 파일이 들어있는 S3 버킷명 |

## 모델·런타임 상수 (코드 고정)

| 상수 | 값 | 위치 |
|---|---|---|
| Gemini 모델 | `gemini-2.5-flash` | `_GEMINI_MODEL` |
| Gemini 요청 timeout | 180초 | `_GEMINI_TIMEOUT_SEC` |
| 오디오 MIME | `audio/mp4` | `_AUDIO_MIME_TYPE` |

## IAM 권한 (Lambda 실행 Role)

- `s3:GetObject` on `arn:aws:s3:::<RECORDING_BUCKET>/call-recordings/*`
- `sqs:SendMessage` on `stt-result-queue`, `stt-lambda-dlq`
- `ssm:GetParameter`, `ssm:GetParameters` on `<GEMINI_API_KEY_PARAMETER_NAME>`
- `AWSLambdaBasicExecutionRole` (CloudWatch Logs)

## 빌드 및 배포

`build.sh` 스크립트로 한 번에:

```bash
cd lambda/call-transcript-processor

# 빌드만 (function.zip 생성)
./build.sh

# 빌드 + AWS Lambda에 자동 업로드
./build.sh --upload

# arm64 함수라면
./build.sh --arch arm64 --upload
```

빌드 결과는 `function.zip`으로 떨어진다. `--upload` 없이 빌드만 했다면 Lambda 콘솔 → `call-transcript-processor` → **코드** 탭 → **업로드 from** → **.zip 파일**로 수동 업로드.

스크립트가 하는 일:
1. `package/`, `function.zip` 정리
2. `--platform manylinux2014_*` 옵션으로 Lambda 실행 환경(Linux)에 맞는 binary wheel 다운로드
3. `lambda_function.py` 복사
4. ZIP 생성
5. `--upload` 시 `aws lambda update-function-code` 실행

## 스모크 테스트

콘솔의 **Test** 탭에서:

```json
{
  "callId": 1,
  "recordings": [
    {"userId": 101, "recordingKey": "call-recordings/1/101/<uuid>"},
    {"userId": 202, "recordingKey": "call-recordings/1/202/<uuid>"}
  ]
}
```

기대 응답:
```json
{"status": "ok", "segmentCount": <int>, "analysesCount": 2}
```

`stt-result-queue`를 receive_message로 polling 하면 위 출력 메시지 섹션 형태의 JSON 1개가 떠 있어야 한다.

## 에러 처리

- 모든 예외는 그대로 raise → Lambda async invocation의 retry attempts(2회)가 자동 동작
- 재시도 모두 실패 시 `stt-lambda-dlq`로 이동 (콘솔 → 비동기 호출 설정)
- 응답 schema 위반은 `_validate_analyses`가 잡아 raise (Gemini가 잘못된 userId를 만들거나 화자가 누락된 경우)
- Gemini 5xx / 429 (RESOURCE_EXHAUSTED)는 재시도로 보통 해결됨

## 비용 모델 (참고)

Gemini 2.5 Flash 가격:
- 텍스트 입력: $0.30 / 1M tokens
- 오디오 입력: $1.00 / 1M tokens (텍스트의 3.33배)
- 출력: $2.50 / 1M tokens

10분 통화 기준 추정:
- 오디오 입력 ~19,200 tokens × $1.00/M ≈ $0.019
- 시스템 프롬프트 ~500 tokens × $0.30/M ≈ $0.0002
- 출력 (transcript ~2,000 + analyses ~1,600) × $2.50/M ≈ $0.009
- **합계 약 $0.028 / 통화 (10분)**

Lambda 실행비: 512MB × 약 10~30초 ≈ $0.0001 / 통화 (무시 가능 수준).

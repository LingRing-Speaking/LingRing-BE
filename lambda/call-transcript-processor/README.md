# call-transcript-processor

LingRing STT 파이프라인의 AWS Lambda 함수. 통화 녹음 m4a 파일 2개를 받아 OpenAI `gpt-4o-transcribe-diarize`로 텍스트 변환한 뒤, 결과를 SQS에 publish한다.

## 흐름

```
Spring (invoke: EVENT) ──> Lambda ──> S3 GET ×2 (병렬)
                                  └─> OpenAI STT ×2 (병렬, gpt-4o-transcribe-diarize)
                                  └─> segments 시간순 머지
                                  └─> SQS publish (stt-result-queue)
```

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

> 필드명 `recordingKey`는 Spring 측 `CallRecording.recordingKey` 컨벤션과 일치한다. 설계 문서 4.3절 명세(`s3Key`)는 실제 코드 컨벤션을 따라 `recordingKey`로 갱신됨.

## 출력 메시지 (Lambda → SQS `stt-result-queue`)

```json
{
  "callId": 1,
  "segments": [
    {"userId": 101, "startSec": 0.0, "endSec": 2.1, "text": "안녕"},
    {"userId": 202, "startSec": 2.5, "endSec": 4.8, "text": "오 안녕"}
  ]
}
```

## 환경 변수

| 키 | 값 예시 |
|---|---|
| `STT_RESULT_QUEUE_URL` | `https://sqs.ap-northeast-2.amazonaws.com/<account>/stt-result-queue` |
| `OPENAI_API_KEY_PARAMETER_NAME` | `/lingring/openai/api-key` |
| `RECORDING_BUCKET` | `lingring-dev` |

> `STT_PROVIDER` 환경변수는 현재 코드에서 직접 참조하지 않는다. 모델은 코드 상수로 고정 (`_STT_MODEL = "gpt-4o-transcribe-diarize"`).

## IAM 권한 (Lambda 실행 Role)

- `s3:GetObject` on `arn:aws:s3:::<RECORDING_BUCKET>/call-recordings/*`
- `sqs:SendMessage` on `stt-result-queue`, `stt-lambda-dlq`
- `ssm:GetParameter`, `ssm:GetParameters` on `/lingring/openai/api-key`
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
{"status": "ok", "segmentCount": <int>}
```

`stt-result-queue`를 receive_message로 polling 하면 위 출력 형태의 JSON 메시지가 1개 떠 있어야 한다.

## 에러 처리

- 모든 예외는 그대로 raise → Lambda async invocation의 retry attempts(2회)가 자동 동작
- 재시도 모두 실패 시 `stt-lambda-dlq`로 이동 (콘솔 → 비동기 호출 설정)
- 일시적 OpenAI 5xx/429는 재시도로 보통 해결됨

## 비용 모델

- 통화 1분당 OpenAI STT ×2회 호출 = $0.012/min (gpt-4o-transcribe-diarize, $0.006/min)
- Lambda 실행비: 512MB × 약 5~10초 × ~$0.0000000083/MB-ms = 통화 1건당 약 $0.00002 (무시 가능)

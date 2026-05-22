"""STT processing Lambda for LingRing call recordings.

Flow:
  1. Spring invokes this function (async/EVENT) with two recording S3 keys
  2. Download both m4a files from S3 in parallel
  3. Call OpenAI gpt-4o-transcribe-diarize on each file in parallel
  4. Merge segments time-ordered with userId attached
  5. Publish single JSON message to stt-result-queue

Expected event shape (from Spring):
  {
    "callId": <long>,
    "recordings": [
      {"userId": <long>, "recordingKey": "call-recordings/<callId>/<userId>/<uuid>"},
      {"userId": <long>, "recordingKey": "call-recordings/<callId>/<userId>/<uuid>"}
    ]
  }
"""

import io
import json
import logging
import os
from concurrent.futures import ThreadPoolExecutor

import boto3
from openai import OpenAI

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Cold-start cached clients reused across warm invocations.
_s3_client = boto3.client("s3")
_sqs_client = boto3.client("sqs")
_ssm_client = boto3.client("ssm")
_openai_client = None

# OpenAI request timeout (seconds). Must fit within Lambda timeout.
_OPENAI_TIMEOUT_SEC = 120
_STT_MODEL = "gpt-4o-transcribe-diarize"
_STT_LANGUAGE = "ko"


def _get_openai_client():
    """Lazy-init OpenAI client. API key fetched from SSM once per container."""
    global _openai_client
    if _openai_client is not None:
        return _openai_client

    parameter_name = os.environ["OPENAI_API_KEY_PARAMETER_NAME"]
    response = _ssm_client.get_parameter(Name=parameter_name, WithDecryption=True)
    api_key = response["Parameter"]["Value"]
    _openai_client = OpenAI(api_key=api_key, timeout=_OPENAI_TIMEOUT_SEC)
    return _openai_client


def _download_from_s3(recording_key):
    """Read S3 object bytes from the configured recording bucket."""
    bucket = os.environ["RECORDING_BUCKET"]
    response = _s3_client.get_object(Bucket=bucket, Key=recording_key)
    return response["Body"].read()


def _transcribe(audio_bytes, file_label):
    """Send one mono audio file to OpenAI gpt-4o-transcribe-diarize.

    Returns: list of {"start": float, "end": float, "text": str}.
    Each file contains a single speaker, so the model's speaker labels
    are ignored downstream; userId mapping is done by file.
    """
    file_obj = io.BytesIO(audio_bytes)
    file_obj.name = "{}.m4a".format(file_label)

    transcript = _get_openai_client().audio.transcriptions.create(
        file=file_obj,
        model=_STT_MODEL,
        response_format="diarized_json",
        chunking_strategy="auto",
        language=_STT_LANGUAGE,
    )

    return [
        {
            "start": float(seg.start),
            "end": float(seg.end),
            "text": seg.text,
        }
        for seg in transcript.segments
    ]


def _process_recording(recording, call_id):
    """Download + transcribe one recording, then attach userId to each segment."""
    user_id = recording["userId"]
    recording_key = recording["recordingKey"]

    logger.info("downloading recording_key=%s for userId=%s", recording_key, user_id)
    audio_bytes = _download_from_s3(recording_key)

    logger.info(
        "transcribing bytes=%d userId=%s callId=%s", len(audio_bytes), user_id, call_id
    )
    raw_segments = _transcribe(
        audio_bytes, file_label="call{}-user{}".format(call_id, user_id)
    )

    return [
        {
            "userId": user_id,
            "startSec": seg["start"],
            "endSec": seg["end"],
            "text": seg["text"],
        }
        for seg in raw_segments
    ]


def _publish_result(payload):
    """Publish merged segments JSON to stt-result-queue."""
    queue_url = os.environ["STT_RESULT_QUEUE_URL"]
    _sqs_client.send_message(
        QueueUrl=queue_url,
        MessageBody=json.dumps(payload, ensure_ascii=False),
    )


def lambda_handler(event, context):
    logger.info("event received: %s", json.dumps(event, ensure_ascii=False))

    call_id = event["callId"]
    recordings = event["recordings"]
    if len(recordings) != 2:
        raise ValueError(
            "expected 2 recordings, got {}".format(len(recordings))
        )

    # Two recordings → run S3 download + STT call concurrently per recording.
    with ThreadPoolExecutor(max_workers=2) as executor:
        per_user_segments = list(
            executor.map(lambda r: _process_recording(r, call_id), recordings)
        )

    merged_segments = [seg for user_segs in per_user_segments for seg in user_segs]
    merged_segments.sort(key=lambda s: s["startSec"])

    result_payload = {"callId": call_id, "segments": merged_segments}
    logger.info(
        "publishing callId=%s segmentCount=%d", call_id, len(merged_segments)
    )
    _publish_result(result_payload)

    return {"status": "ok", "segmentCount": len(merged_segments)}

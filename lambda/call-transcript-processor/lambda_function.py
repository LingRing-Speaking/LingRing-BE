"""Gemini Lambda for LingRing call analysis.

A single Gemini multimodal call performs both transcription and per-user
feedback analysis. Replaces the previous two-hop OpenAI STT + Chat flow.

Flow:
  1. Spring invokes this function (async/EVENT) with two recording S3 keys
  2. Download both m4a files from S3 in parallel
  3. Call Gemini once with both audio files + JSON schema
     -> transcript (time-ordered, userId-tagged) + per-user feedback x 2
  4. Publish single JSON message to SQS

Expected event shape (unchanged from previous version):
  {
    "callId": <long>,
    "recordings": [
      {"userId": <long>, "recordingKey": "call-recordings/<callId>/<userId>/<uuid>"},
      {"userId": <long>, "recordingKey": "call-recordings/<callId>/<userId>/<uuid>"}
    ]
  }
"""

import json
import logging
import os
from concurrent.futures import ThreadPoolExecutor
from enum import Enum

import boto3
from google import genai
from google.genai import types
from pydantic import BaseModel

logger = logging.getLogger()
logger.setLevel(logging.INFO)

# Cold-start cached clients reused across warm invocations.
_s3_client = boto3.client("s3")
_sqs_client = boto3.client("sqs")
_ssm_client = boto3.client("ssm")
_gemini_client = None

_GEMINI_MODEL = "gemini-2.5-flash"
_GEMINI_TIMEOUT_SEC = 180
# m4a files are MP4 containers carrying AAC audio; audio/mp4 is the canonical MIME.
_AUDIO_MIME_TYPE = "audio/mp4"


class FeedbackTag(str, Enum):
    GRAMMAR = "GRAMMAR"
    VOCABULARY = "VOCABULARY"
    COLLOCATION = "COLLOCATION"
    TONE = "TONE"
    OTHER = "OTHER"


class TranscriptSegment(BaseModel):
    userId: int
    startSec: float
    endSec: float
    text: str


class Transcript(BaseModel):
    segments: list[TranscriptSegment]


class MistakeItem(BaseModel):
    tag: FeedbackTag
    wrong: str
    improved: str
    reason: str
    koMeaning: str


class PositiveItem(BaseModel):
    sentence: str
    goodPart: str
    koMeaning: str


class UserAnalysis(BaseModel):
    userId: int
    mistakes: list[MistakeItem]
    positives: list[PositiveItem]


class GeminiResponse(BaseModel):
    transcript: Transcript
    analyses: list[UserAnalysis]


_SYSTEM_INSTRUCTION = """\
You are an English coach for Korean learners. You will receive two audio files
of a phone call between two speakers. Each file contains exactly one speaker.

Your task:
  1. Transcribe both audio files into a single time-ordered transcript. Each
     segment must be tagged with the userId of the speaker who said it
     (mapping provided in the user message).
  2. INDEPENDENTLY analyze each of the two speakers' English utterances and
     produce a feedback object containing BOTH `mistakes` and `positives`.
     Apply identical evaluation standards to both speakers.

Independence and symmetry rules (CRITICAL):
  - Evaluate each speaker as if you do NOT know what the other said. Never
    grade one speaker relative to the other.
  - If the SAME grammatical error, awkward collocation, or unnatural phrasing
    appears in BOTH speakers' utterances (e.g., both say "in this weekend"),
    it MUST be flagged in BOTH users' `mistakes` arrays with consistent
    wording. Never let one speaker's strength make you overlook the same
    weakness in the other.
  - For EACH speaker, examine BOTH categories — do not return a result where
    one speaker has only `mistakes` and the other has only `positives` unless
    that asymmetry is genuinely justified by the audio (e.g., one speaker
    spoke only 1-2 words).
  - Do not include the same utterance in a speaker's `positives` if it
    contains an error you flagged in their (or the other speaker's) `mistakes`.

Output rules:
  - Output ONLY a JSON object matching the provided schema. No prose outside JSON.
  - `analyses` MUST contain exactly two objects, one per userId in the mapping.
  - `mistakes[].reason`, `mistakes[].koMeaning`, `positives[].goodPart`,
    `positives[].koMeaning` MUST be written in Korean.
  - Skip utterances of 1-2 words (too short to analyze).
  - Skip purely Korean utterances. If a speaker mixed languages, focus on English parts.
  - If a speaker has nothing significant, return empty arrays for `mistakes` or
    `positives` for that user.
  - tag MUST be one of: GRAMMAR, VOCABULARY, COLLOCATION, TONE, OTHER.
"""


def _get_gemini_client():
    """Lazy-init Gemini client. API key fetched from SSM once per container."""
    global _gemini_client
    if _gemini_client is not None:
        return _gemini_client

    parameter_name = os.environ["GEMINI_API_KEY_PARAMETER_NAME"]
    response = _ssm_client.get_parameter(Name=parameter_name, WithDecryption=True)
    api_key = response["Parameter"]["Value"]
    _gemini_client = genai.Client(
        api_key=api_key,
        http_options=types.HttpOptions(timeout=_GEMINI_TIMEOUT_SEC * 1000),
    )
    return _gemini_client


def _download_from_s3(recording_key):
    """Read S3 object bytes from the configured recording bucket."""
    bucket = os.environ["RECORDING_BUCKET"]
    response = _s3_client.get_object(Bucket=bucket, Key=recording_key)
    return response["Body"].read()


def _build_contents(recordings, audio_bytes_list):
    """Interleave audio Parts with userId labels so Gemini can map speakers."""
    parts = []
    for recording, audio_bytes in zip(recordings, audio_bytes_list):
        parts.append("Audio file for userId={}:".format(recording["userId"]))
        parts.append(
            types.Part.from_bytes(data=audio_bytes, mime_type=_AUDIO_MIME_TYPE)
        )

    parts.append(
        "Speaker mapping: {}".format(
            ", ".join("userId={}".format(r["userId"]) for r in recordings)
        )
    )
    parts.append(
        "Produce the combined transcript and per-user feedback as defined by "
        "the response schema."
    )
    return parts


def _call_gemini(contents):
    response = _get_gemini_client().models.generate_content(
        model=_GEMINI_MODEL,
        contents=contents,
        config=types.GenerateContentConfig(
            system_instruction=_SYSTEM_INSTRUCTION,
            response_mime_type="application/json",
            response_schema=GeminiResponse,
        ),
    )
    return GeminiResponse.model_validate_json(response.text)


def _validate_analyses(parsed, recordings):
    """Reject responses where Gemini swapped or invented userIds."""
    expected = {r["userId"] for r in recordings}
    actual = {a.userId for a in parsed.analyses}
    if expected != actual:
        raise ValueError(
            "analyses userId set mismatch: expected={} actual={}".format(
                sorted(expected), sorted(actual)
            )
        )

    segment_user_ids = {s.userId for s in parsed.transcript.segments}
    unknown_in_segments = segment_user_ids - expected
    if unknown_in_segments:
        raise ValueError(
            "transcript contains unknown userIds: {}".format(sorted(unknown_in_segments))
        )


def _publish_result(payload):
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
        raise ValueError("expected 2 recordings, got {}".format(len(recordings)))

    # S3 GETs are independent; run them concurrently to shave a round-trip.
    with ThreadPoolExecutor(max_workers=2) as executor:
        audio_bytes_list = list(
            executor.map(lambda r: _download_from_s3(r["recordingKey"]), recordings)
        )

    logger.info(
        "calling Gemini callId=%s model=%s audioBytes=%s",
        call_id,
        _GEMINI_MODEL,
        [len(b) for b in audio_bytes_list],
    )
    contents = _build_contents(recordings, audio_bytes_list)
    parsed = _call_gemini(contents)
    _validate_analyses(parsed, recordings)

    result_payload = {
        "callId": call_id,
        "modelIdentifier": _GEMINI_MODEL,
        "transcript": parsed.transcript.model_dump(),
        "analyses": [a.model_dump(mode="json") for a in parsed.analyses],
    }
    logger.info(
        "publishing callId=%s segmentCount=%d analysesCount=%d",
        call_id,
        len(parsed.transcript.segments),
        len(parsed.analyses),
    )
    _publish_result(result_payload)

    return {
        "status": "ok",
        "segmentCount": len(parsed.transcript.segments),
        "analysesCount": len(parsed.analyses),
    }

#!/usr/bin/env bash
# Build (and optionally deploy) the call-transcript-processor Lambda.
#
# Usage:
#   ./build.sh                  # build for x86_64 → function.zip
#   ./build.sh --arch arm64     # build for arm64
#   ./build.sh --upload         # build + upload via AWS CLI
#   ./build.sh --upload --arch arm64
#
# Requires: python3 + pip, zip, (optional) aws CLI

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

ARCH="x86_64"
PYTHON_VERSION="3.12"
FUNCTION_NAME="call-transcript-processor"
REGION="ap-northeast-2"
DO_UPLOAD=0

while [[ $# -gt 0 ]]; do
    case "$1" in
        --arch)
            ARCH="$2"
            shift 2
            ;;
        --upload)
            DO_UPLOAD=1
            shift
            ;;
        --help|-h)
            grep '^#' "$0" | sed 's/^# \?//'
            exit 0
            ;;
        *)
            echo "Unknown option: $1" >&2
            echo "Run with --help for usage." >&2
            exit 1
            ;;
    esac
done

case "$ARCH" in
    x86_64) PLATFORM="manylinux2014_x86_64" ;;
    arm64)  PLATFORM="manylinux2014_aarch64" ;;
    *)
        echo "Invalid --arch: $ARCH (must be x86_64 or arm64)" >&2
        exit 1
        ;;
esac

echo "==> Cleaning previous build"
rm -rf package function.zip

echo "==> Installing dependencies (platform=$PLATFORM python=$PYTHON_VERSION)"
python3 -m pip install \
    --platform "$PLATFORM" \
    --target ./package \
    --implementation cp \
    --python-version "$PYTHON_VERSION" \
    --only-binary=:all: \
    --upgrade \
    --quiet \
    -r requirements.txt

echo "==> Copying source files"
cp lambda_function.py ./package/

echo "==> Creating ZIP"
( cd package && zip -rq ../function.zip . )

ZIP_SIZE="$(du -h function.zip | cut -f1)"
echo "==> Built: $(pwd)/function.zip ($ZIP_SIZE)"

if [[ "$DO_UPLOAD" -eq 1 ]]; then
    if ! command -v aws >/dev/null 2>&1; then
        echo "aws CLI not found in PATH. Install it or upload via console." >&2
        exit 1
    fi
    echo "==> Uploading to Lambda $FUNCTION_NAME (region=$REGION)"
    aws lambda update-function-code \
        --function-name "$FUNCTION_NAME" \
        --zip-file "fileb://function.zip" \
        --region "$REGION" \
        --no-cli-pager > /dev/null
    echo "==> Deployed."
fi

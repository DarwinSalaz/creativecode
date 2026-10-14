#!/usr/bin/env bash

set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="${PROJECT_DIR}/.env"

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "Missing ${ENV_FILE}. Create it from .env.example." >&2
  exit 1
fi

set -a
source "${ENV_FILE}"
set +a

if [[ -z "${DB_URL:-}" || -z "${DB_USER:-}" || -z "${DB_PASSWORD:-}" ]]; then
  echo "DB_URL, DB_USER and DB_PASSWORD are required in ${ENV_FILE}." >&2
  exit 1
fi

cd "${PROJECT_DIR}"
LOCAL_JAVA="${PROJECT_DIR}/.local/jdks/temurin8-x64/Contents/Home"
if [[ -d "${LOCAL_JAVA}" ]]; then
  export JAVA_HOME="${LOCAL_JAVA}"
fi
exec ./gradlew bootRun

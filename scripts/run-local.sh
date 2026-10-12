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

if [[ -z "${DB_PW:-}" ]]; then
  echo "DB_PW is required in ${ENV_FILE}." >&2
  exit 1
fi

cd "${PROJECT_DIR}"
exec ./gradlew bootRun

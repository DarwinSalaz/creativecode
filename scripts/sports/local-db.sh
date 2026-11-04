#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DATA="$ROOT/.local/sports-postgres"
SOCKET="$ROOT/.local/sports-postgres-socket"
case "${1:-status}" in
  start)
    mkdir -p "$SOCKET"
    chmod 700 "$SOCKET"
    if [[ ! -f "$DATA/PG_VERSION" ]]; then
      initdb -D "$DATA" --auth-local=trust --auth-host=scram-sha-256 --encoding=UTF8 --locale=C
    fi
    if pg_ctl -D "$DATA" status >/dev/null 2>&1; then echo 'Local sports PostgreSQL already running'; else
      pg_ctl -D "$DATA" -l "$ROOT/.local/sports-postgres.log" -o "-h 127.0.0.1 -p 55432 -k $SOCKET" -w start
    fi
    ;;
  stop) pg_ctl -D "$DATA" -m fast -w stop ;;
  status) pg_ctl -D "$DATA" status ;;
  *) echo 'Usage: local-db.sh start|stop|status' >&2; exit 2 ;;
esac

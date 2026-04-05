#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FRONTEND_DIR="$PROJECT_ROOT/frontend"

for required_command in node npm; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "Required command not found: $required_command" >&2
    exit 1
  fi
done

if [[ ! -d "$FRONTEND_DIR/node_modules" ]]; then
  (
    cd "$FRONTEND_DIR"
    npm install --legacy-peer-deps --cache .npm-cache
  )
fi

(
  cd "$FRONTEND_DIR"
  CI=1 NG_CLI_ANALYTICS=false npm start -- --host 0.0.0.0
)

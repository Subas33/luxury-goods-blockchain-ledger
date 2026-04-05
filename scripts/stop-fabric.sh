#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FABRIC_WORKDIR="${FABRIC_WORKDIR:-$PROJECT_ROOT/.fabric}"
FABRIC_SAMPLES_DIR="${FABRIC_SAMPLES_DIR:-$FABRIC_WORKDIR/fabric-samples}"

(
  cd "$FABRIC_SAMPLES_DIR/test-network"
  ./network.sh down
)


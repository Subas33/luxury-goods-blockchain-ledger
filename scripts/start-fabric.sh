#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FABRIC_WORKDIR="${FABRIC_WORKDIR:-$PROJECT_ROOT/.fabric}"
FABRIC_SAMPLES_DIR="${FABRIC_SAMPLES_DIR:-$FABRIC_WORKDIR/fabric-samples}"
FABRIC_VERSION="${FABRIC_VERSION:-2.5.15}"
CA_VERSION="${CA_VERSION:-1.5.17}"
CHANNEL_NAME="${CHANNEL_NAME:-mychannel}"

for required_command in curl git docker; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "Required command not found: $required_command" >&2
    exit 1
  fi
done

if [[ ! -d "$FABRIC_SAMPLES_DIR/test-network" ]]; then
  mkdir -p "$FABRIC_WORKDIR"
  INSTALL_SCRIPT="$FABRIC_WORKDIR/install-fabric.sh"
  curl -sSL https://raw.githubusercontent.com/hyperledger/fabric/main/scripts/install-fabric.sh -o "$INSTALL_SCRIPT"
  chmod +x "$INSTALL_SCRIPT"
  (
    cd "$FABRIC_WORKDIR"
    "$INSTALL_SCRIPT" --fabric-version "$FABRIC_VERSION" --ca-version "$CA_VERSION" docker binary samples
  )
fi

(
  cd "$FABRIC_SAMPLES_DIR/test-network"
  ./network.sh up createChannel -c "$CHANNEL_NAME" -ca
)

#!/usr/bin/env bash
set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
FABRIC_WORKDIR="${FABRIC_WORKDIR:-$PROJECT_ROOT/.fabric}"
FABRIC_SAMPLES_DIR="${FABRIC_SAMPLES_DIR:-$FABRIC_WORKDIR/fabric-samples}"
CHAINCODE_DIR="${CHAINCODE_DIR:-$PROJECT_ROOT/chaincode}"
CHANNEL_NAME="${CHANNEL_NAME:-mychannel}"
CHAINCODE_NAME="${CHAINCODE_NAME:-luxuryasset}"
CHAINCODE_VERSION="${CHAINCODE_VERSION:-1.0}"
CHAINCODE_SEQUENCE="${CHAINCODE_SEQUENCE:-1}"
FABRIC_VERSION="${FABRIC_VERSION:-2.5.15}"
FABRIC_JAVAENV_TAG="${FABRIC_JAVAENV_TAG:-${FABRIC_VERSION%.*}}"
export CHAINCODE_DIST_NAME="${CHAINCODE_DIST_NAME:-$CHAINCODE_NAME}"

for required_command in java docker curl; do
  if ! command -v "$required_command" >/dev/null 2>&1; then
    echo "Required command not found: $required_command" >&2
    exit 1
  fi
done

if [[ ! -d "$FABRIC_SAMPLES_DIR/test-network" ]]; then
  echo "Fabric samples not found. Run ./scripts/start-fabric.sh first." >&2
  exit 1
fi

if ! docker image inspect "hyperledger/fabric-javaenv:$FABRIC_JAVAENV_TAG" >/dev/null 2>&1; then
  docker pull "hyperledger/fabric-javaenv:$FABRIC_JAVAENV_TAG"
fi

(
  cd "$PROJECT_ROOT"
  ./mvnw -pl chaincode -am clean package
)

(
  cd "$FABRIC_SAMPLES_DIR/test-network"
  ./network.sh deployCC \
    -c "$CHANNEL_NAME" \
    -ccn "$CHAINCODE_NAME" \
    -ccv "$CHAINCODE_VERSION" \
    -ccs "$CHAINCODE_SEQUENCE" \
    -ccp "$CHAINCODE_DIR" \
    -ccl java
)

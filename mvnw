#!/usr/bin/env sh
set -eu

BASE_DIR="$(
  CDPATH= cd -- "$(dirname -- "$0")" && pwd
)"
PROPERTIES_FILE="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

if [ ! -f "$PROPERTIES_FILE" ]; then
  echo "Missing Maven wrapper properties file: $PROPERTIES_FILE" >&2
  exit 1
fi

DISTRIBUTION_URL="$(
  awk -F= '/^distributionUrl=/{print substr($0, index($0, "=") + 1); exit}' "$PROPERTIES_FILE"
)"

if [ -z "${DISTRIBUTION_URL:-}" ]; then
  echo "distributionUrl is not configured in $PROPERTIES_FILE" >&2
  exit 1
fi

DISTRIBUTION_FILE="$(basename "$DISTRIBUTION_URL")"
MAVEN_DIR_NAME="${DISTRIBUTION_FILE%-bin.zip}"
MAVEN_HOME="$BASE_DIR/.mvn/wrapper/dists/$MAVEN_DIR_NAME"
MVN_BIN="$MAVEN_HOME/bin/mvn"

if [ ! -x "$MVN_BIN" ]; then
  mkdir -p "$BASE_DIR/.mvn/wrapper/dists" "$BASE_DIR/.mvn/wrapper/tmp"
  ARCHIVE_PATH="$BASE_DIR/.mvn/wrapper/tmp/$DISTRIBUTION_FILE"

  if [ ! -f "$ARCHIVE_PATH" ]; then
    if command -v curl >/dev/null 2>&1; then
      curl -fsSL "$DISTRIBUTION_URL" -o "$ARCHIVE_PATH"
    elif command -v wget >/dev/null 2>&1; then
      wget -qO "$ARCHIVE_PATH" "$DISTRIBUTION_URL"
    else
      echo "Neither curl nor wget is available to download Maven." >&2
      exit 1
    fi
  fi

  rm -rf "$MAVEN_HOME"
  if command -v unzip >/dev/null 2>&1; then
    unzip -q "$ARCHIVE_PATH" -d "$BASE_DIR/.mvn/wrapper/dists"
  else
    (
      cd "$BASE_DIR/.mvn/wrapper/dists"
      jar xf "$ARCHIVE_PATH"
    )
  fi
  chmod +x "$MVN_BIN"
fi

exec "$MVN_BIN" "$@"

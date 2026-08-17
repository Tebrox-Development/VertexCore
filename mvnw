#!/bin/sh
set -eu

BASE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
PROPS="$BASE_DIR/.mvn/wrapper/maven-wrapper.properties"

read_prop() {
  sed -n "s/^$1=//p" "$PROPS" | head -n 1 | tr -d '\r'
}

DIST_URL=$(read_prop distributionUrl)
DIST_SHA=$(read_prop distributionSha256Sum)
[ -n "$DIST_URL" ] || { echo "Missing distributionUrl in $PROPS" >&2; exit 1; }

ARCHIVE=${DIST_URL##*/}
DIST_NAME=${ARCHIVE%-bin.zip}
MAVEN_USER_HOME=${MAVEN_USER_HOME:-"$HOME/.m2"}
MAVEN_HOME="$MAVEN_USER_HOME/wrapper/dists/$DIST_NAME"

if [ ! -x "$MAVEN_HOME/bin/mvn" ]; then
  TMP_DIR=$(mktemp -d)
  trap 'rm -rf "$TMP_DIR"' EXIT HUP INT TERM
  ZIP="$TMP_DIR/$ARCHIVE"

  echo "Maven Wrapper: downloading $DIST_URL" >&2
  if command -v curl >/dev/null 2>&1; then
    curl -fsSL "$DIST_URL" -o "$ZIP"
  elif command -v wget >/dev/null 2>&1; then
    wget -q "$DIST_URL" -O "$ZIP"
  else
    echo "Maven Wrapper requires curl or wget for the first download." >&2
    exit 1
  fi

  if [ -n "$DIST_SHA" ]; then
    if command -v sha256sum >/dev/null 2>&1; then
      printf '%s  %s\n' "$DIST_SHA" "$ZIP" | sha256sum -c - >/dev/null
    elif command -v shasum >/dev/null 2>&1; then
      ACTUAL=$(shasum -a 256 "$ZIP" | awk '{print $1}')
      [ "$ACTUAL" = "$DIST_SHA" ] || { echo "Maven distribution checksum mismatch." >&2; exit 1; }
    else
      echo "Cannot validate Maven distribution: sha256sum/shasum missing." >&2
      exit 1
    fi
  fi

  command -v unzip >/dev/null 2>&1 || { echo "Maven Wrapper requires unzip for the first installation." >&2; exit 1; }
  mkdir -p "$TMP_DIR/unpacked" "$(dirname "$MAVEN_HOME")"
  unzip -q "$ZIP" -d "$TMP_DIR/unpacked"
  EXTRACTED="$TMP_DIR/unpacked/$DIST_NAME"
  [ -x "$EXTRACTED/bin/mvn" ] || { echo "Unexpected Maven archive layout." >&2; exit 1; }
  rm -rf "$MAVEN_HOME"
  mv "$EXTRACTED" "$MAVEN_HOME"
fi

exec "$MAVEN_HOME/bin/mvn" "$@"

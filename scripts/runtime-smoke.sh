#!/usr/bin/env bash
set -euo pipefail

PAPER_VERSION="26.2"
PAPER_BUILD="121"
USER_AGENT="VertexCore-runtime-smoke/1.0 (https://github.com/Tebrox-Development/VertexCore)"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="${ROOT_DIR}/target/runtime-smoke"
SERVER_DIR="${RUNTIME_DIR}/server"
LOG_FILE="${SERVER_DIR}/logs/latest.log"
BUILD_JSON="${RUNTIME_DIR}/paper-builds.json"
PLUGIN_JAR="${ROOT_DIR}/target/vertexCore-1.1.0-SNAPSHOT.jar"

rm -rf "${RUNTIME_DIR}"
mkdir -p "${SERVER_DIR}/plugins"

if [[ ! -f "${PLUGIN_JAR}" ]]; then
  echo "VertexCore build artifact not found: ${PLUGIN_JAR}" >&2
  exit 1
fi

curl --fail --silent --show-error --location \
  --header "User-Agent: ${USER_AGENT}" \
  "https://fill.papermc.io/v3/projects/paper/versions/${PAPER_VERSION}/builds" \
  --output "${BUILD_JSON}"

PAPER_URL="$(python3 - "${BUILD_JSON}" "${PAPER_BUILD}" <<'PY'
import json
import sys

path, build_id = sys.argv[1], int(sys.argv[2])
with open(path, encoding="utf-8") as handle:
    builds = json.load(handle)

build = next((entry for entry in builds if entry.get("id") == build_id), None)
if build is None:
    raise SystemExit(f"Pinned Paper build {build_id} not found")
if build.get("channel") != "STABLE":
    raise SystemExit(f"Pinned Paper build {build_id} is not STABLE")

download = build.get("downloads", {}).get("server:default")
if not download or not download.get("url"):
    raise SystemExit(f"Pinned Paper build {build_id} has no server:default download")
print(download["url"])
PY
)"

curl --fail --silent --show-error --location \
  --header "User-Agent: ${USER_AGENT}" \
  "${PAPER_URL}" \
  --output "${SERVER_DIR}/paper.jar"

cp "${PLUGIN_JAR}" "${SERVER_DIR}/plugins/VertexCore.jar"
printf 'eula=true\n' > "${SERVER_DIR}/eula.txt"
mkfifo "${SERVER_DIR}/console.in"

pushd "${SERVER_DIR}" >/dev/null
exec 3<>console.in
java -Xms512M -Xmx1024M -jar paper.jar --nogui <console.in >server-console.log 2>&1 &
SERVER_PID=$!
popd >/dev/null

cleanup() {
  if kill -0 "${SERVER_PID}" 2>/dev/null; then
    printf 'stop\n' >&3 || true
    for _ in $(seq 1 30); do
      kill -0 "${SERVER_PID}" 2>/dev/null || return 0
      sleep 1
    done
    kill "${SERVER_PID}" 2>/dev/null || true
  fi
}
trap cleanup EXIT

READY=0
for _ in $(seq 1 120); do
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    echo "Paper exited before reaching ready state" >&2
    cat "${SERVER_DIR}/server-console.log" >&2 || true
    exit 1
  fi

  if [[ -f "${LOG_FILE}" ]] && grep -Fq 'VertexCore enabled.' "${LOG_FILE}" && grep -Eq 'Done \([0-9.]+s\)! For help, type "help"' "${LOG_FILE}"; then
    READY=1
    break
  fi
  sleep 1
done

if [[ "${READY}" -ne 1 ]]; then
  echo "Timed out waiting for Paper 26.2 and VertexCore to become ready" >&2
  cat "${SERVER_DIR}/server-console.log" >&2 || true
  exit 1
fi

if grep -Eiq '(Could not load.*VertexCore|Error occurred while enabling VertexCore|Exception.*VertexCore|Could not pass event.*VertexCore)' "${LOG_FILE}"; then
  echo "VertexCore startup error detected" >&2
  cat "${LOG_FILE}" >&2
  exit 1
fi

printf 'stop\n' >&3
for _ in $(seq 1 60); do
  if ! kill -0 "${SERVER_PID}" 2>/dev/null; then
    wait "${SERVER_PID}"
    trap - EXIT
    break
  fi
  sleep 1
done

if kill -0 "${SERVER_PID}" 2>/dev/null; then
  echo "Paper did not stop within 60 seconds" >&2
  exit 1
fi

if ! grep -Fq 'VertexCore disabled.' "${LOG_FILE}"; then
  echo "VertexCore disable marker missing" >&2
  cat "${LOG_FILE}" >&2
  exit 1
fi

if grep -Eiq '(Error occurred while disabling VertexCore|Exception.*VertexCore)' "${LOG_FILE}"; then
  echo "VertexCore shutdown error detected" >&2
  cat "${LOG_FILE}" >&2
  exit 1
fi

echo "Paper ${PAPER_VERSION} build ${PAPER_BUILD} runtime smoke passed."

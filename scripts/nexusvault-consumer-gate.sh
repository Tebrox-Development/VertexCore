#!/usr/bin/env bash
set -euo pipefail

NEXUSVAULT_REPOSITORY="Tebrox-Development/NexusVault"
NEXUSVAULT_BRANCH="dev"
ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
WORK_DIR="${ROOT_DIR}/target/nexusvault-consumer-gate"
VERTEXCORE_JAR="${ROOT_DIR}/target/vertexCore-1.1.0-SNAPSHOT.jar"

rm -rf "${WORK_DIR}"
mkdir -p "${WORK_DIR}"

if [[ ! -f "${VERTEXCORE_JAR}" ]]; then
  echo "VertexCore build artifact not found: ${VERTEXCORE_JAR}" >&2
  exit 1
fi

mvn -B -ntp org.apache.maven.plugins:maven-install-plugin:3.1.4:install-file \
  -Dfile="${VERTEXCORE_JAR}" \
  -DgroupId=com.github.Tebrox \
  -DartifactId=VertexCore \
  -Dversion=development-SNAPSHOT \
  -Dpackaging=jar \
  -DgeneratePom=true

NEXUSVAULT_DIR="${WORK_DIR}/NexusVault"
GIT_TERMINAL_PROMPT=0 git clone --quiet --depth 1 --branch "${NEXUSVAULT_BRANCH}" \
  "https://github.com/${NEXUSVAULT_REPOSITORY}.git" "${NEXUSVAULT_DIR}"

NEXUSVAULT_SHA="$(git -C "${NEXUSVAULT_DIR}" rev-parse HEAD)"
echo "NexusVault consumer ref: ${NEXUSVAULT_REPOSITORY}@${NEXUSVAULT_BRANCH} (${NEXUSVAULT_SHA})"
echo "VertexCore consumer artifact: com.github.Tebrox:VertexCore:development-SNAPSHOT"

mvn -B -ntp -f "${NEXUSVAULT_DIR}/pom.xml" verify

echo "NexusVault consumer compatibility gate passed for ${NEXUSVAULT_SHA}."

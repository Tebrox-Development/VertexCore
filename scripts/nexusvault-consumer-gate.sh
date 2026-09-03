#!/usr/bin/env bash
set -euo pipefail

NEXUSVAULT_DEV_SHA="412becf44e6de104cfb0804f7735ff012516c0cb"
CONTRACT_TEST="de.tebrox.vertexCore.compat.NexusVaultConsumerCompatibilityTest"

echo "NexusVault consumer snapshot: dev@${NEXUSVAULT_DEV_SHA}"
echo "Compatibility mode: embedded read-only contract; no NexusVault checkout"
echo "VertexCore consumer contract: ${CONTRACT_TEST}"

mvn -B -ntp -Dtest="${CONTRACT_TEST}" test

echo "NexusVault consumer compatibility contract passed for ${NEXUSVAULT_DEV_SHA}."

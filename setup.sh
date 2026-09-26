#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
# JDK 21 builds JVM 17 bytecode. The wrapper resolves pinned dependencies.
./gradlew :core:test :mcp:test :desktop:compileKotlin --console=plain
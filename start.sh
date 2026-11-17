#!/usr/bin/env bash
# Builds and tests all modules, then starts the application.
set -euo pipefail
cd "$(dirname "$0")"

./mvnw install
./mvnw -pl barcode-label-app javafx:run

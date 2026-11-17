#!/usr/bin/env bash
# Linux/macOS counterpart of verify-phase6.ps1: installs every phase in dependency order and runs
# all tests. The JavaFX UI tests need a display; on a headless machine run it as
#   xvfb-run -a ./verify-phase6.sh
set -euo pipefail
cd "$(dirname "$0")"

mvn() { ./mvnw -B "$@"; }

mvn clean install
mvn -f phase2-pom.xml clean install
mvn -N -f phase3-pom.xml install
mvn -N -f phase3-build-pom.xml install
mvn -f phase3-final-pom.xml clean install
mvn -f phase4-final-pom.xml clean install
mvn -f phase5-pom.xml -pl barcode-label-workflow -am clean install
mvn -f phase5-fixed-pom.xml clean install
mvn -f phase6-final-pom.xml clean install

echo 'Phase 6 verification completed successfully.'

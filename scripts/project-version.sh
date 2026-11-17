#!/usr/bin/env bash
# Prints the project version from the root pom.xml: the <version> directly under <project>.
set -euo pipefail
cd "$(dirname "$0")/.."
version=$(sed -n 's#^    <version>\([^<]*\)</version>.*#\1#p' pom.xml | head -n 1)
[ -n "$version" ] || { echo "No project version found in pom.xml" >&2; exit 1; }
echo "$version"

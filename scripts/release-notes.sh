#!/usr/bin/env bash
# Prints the CHANGELOG.md section for a version (without its heading), for a GitHub release's notes.
# Fails when the changelog has no section for that version, so a release cannot go out undocumented.
set -euo pipefail
cd "$(dirname "$0")/.."
version="${1:?usage: scripts/release-notes.sh <version>}"
notes=$(awk -v heading="## [$version]" '
    index($0, heading) == 1 { inside = 1; next }
    inside && /^## \[/ { exit }
    inside { print }
' CHANGELOG.md)
[ -n "$(printf '%s' "$notes" | tr -d '[:space:]')" ] || { echo "CHANGELOG.md has no section for $version" >&2; exit 1; }
printf '%s\n' "$notes" | sed -e '/./,$!d'

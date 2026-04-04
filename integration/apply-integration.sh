#!/usr/bin/env bash
# Merge Withings ScanWatch add-on into a Gadgetbridge checkout (see ../README.md).
set -euo pipefail

ADDON_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PATCH_FILE="${ADDON_ROOT}/integration/gadgetbridge-integration.patch"
JAVA_SRC="${ADDON_ROOT}/module/src/main/java"

GB_ROOT="${1:-${GADGETBRIDGE_DIR:-}}"

if [[ -z "${GB_ROOT}" ]]; then
  echo "Usage: $0 /path/to/Gadgetbridge" >&2
  echo "Or:    GADGETBRIDGE_DIR=/path/to/Gadgetbridge $0" >&2
  exit 1
fi

if [[ ! -d "${GB_ROOT}/app/src/main/java" ]]; then
  echo "error: ${GB_ROOT} does not look like a Gadgetbridge tree (missing app/src/main/java)" >&2
  exit 1
fi

if [[ ! -f "${PATCH_FILE}" ]]; then
  echo "error: missing ${PATCH_FILE}" >&2
  exit 1
fi

echo "Copying add-on Java sources into ${GB_ROOT}/app/src/main/java ..."
rsync -a "${JAVA_SRC}/" "${GB_ROOT}/app/src/main/java/"

echo "Applying ${PATCH_FILE} ..."
if git -C "${GB_ROOT}" rev-parse --git-dir >/dev/null 2>&1; then
  git -C "${GB_ROOT}" apply "${PATCH_FILE}"
else
  (cd "${GB_ROOT}" && patch -p1 --forward < "${PATCH_FILE}")
fi

echo "Done. Build with: (cd \"${GB_ROOT}\" && ./gradlew :app:assembleDebug)"

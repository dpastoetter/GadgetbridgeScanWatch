#!/usr/bin/env bash
# Merge Withings ScanWatch add-on into a Gadgetbridge checkout (see ../README.md).
set -euo pipefail

ADDON_ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PATCH_FILE="${ADDON_ROOT}/integration/gadgetbridge-integration.patch"
JAVA_SRC="${ADDON_ROOT}/module/src/main/java"

# Only these trees are copied. Anything else in the add-on sources is refused so a
# tampered tree cannot overwrite unrelated Gadgetbridge files.
ALLOWED_PACKAGES=(
  "nodomain/freeyourgadget/gadgetbridge/devices/withingsscanwatch"
  "nodomain/freeyourgadget/gadgetbridge/service/devices/withingsscanwatch"
)

ALLOWED_PATCH_PATHS=(
  "app/src/main/java/nodomain/freeyourgadget/gadgetbridge/model/DeviceType.java"
  "app/src/main/res/values/strings.xml"
)

reject_unsafe_path() {
  local path="$1"
  local label="$2"
  if [[ -z "${path}" || "${path}" == -* || "${path}" == *:* || "${path}" == *$'\n'* || "${path}" == *$'\r'* ]]; then
    echo "error: ${label} is empty or not a safe local path" >&2
    exit 1
  fi
}

assert_inside() {
  local child="$1"
  local parent="$2"
  local label="$3"
  case "${child}" in
    "${parent}"/*) ;;
    *)
      echo "error: ${label} resolves outside ${parent}" >&2
      exit 1
      ;;
  esac
}

verify_patch_paths() {
  local line path allowed ok
  if grep -q '\.\./' "${PATCH_FILE}"; then
    echo "error: patch contains path traversal" >&2
    exit 1
  fi
  while IFS= read -r line || [[ -n "${line}" ]]; do
    case "${line}" in
      '+++ '*)
        path="${line#+++ }"
        path="${path%%$'\t'*}"
        if [[ "${path}" == "/dev/null" ]]; then
          continue
        fi
        path="${path#b/}"
        ok=0
        for allowed in "${ALLOWED_PATCH_PATHS[@]}"; do
          if [[ "${path}" == "${allowed}" ]]; then
            ok=1
            break
          fi
        done
        if [[ "${ok}" -ne 1 ]]; then
          echo "error: patch touches unexpected path: ${path}" >&2
          exit 1
        fi
        ;;
    esac
  done < "${PATCH_FILE}"
}

verify_source_tree() {
  local file rel ok pkg
  if [[ -n "$(find "${JAVA_SRC}" -type l -print -quit)" ]]; then
    echo "error: refusing to copy symbolic links from the add-on sources" >&2
    exit 1
  fi
  while IFS= read -r -d '' file; do
    rel="${file#"${JAVA_SRC}/"}"
    case "${rel}" in
      *..*)
        echo "error: refusing path traversal in source: ${rel}" >&2
        exit 1
        ;;
    esac
    ok=0
    for pkg in "${ALLOWED_PACKAGES[@]}"; do
      case "${rel}" in
        "${pkg}"/*)
          ok=1
          break
          ;;
      esac
    done
    if [[ "${ok}" -ne 1 ]]; then
      echo "error: refusing to copy unexpected source: ${rel}" >&2
      exit 1
    fi
  done < <(find "${JAVA_SRC}" -type f -print0)
}

GB_ROOT="${1:-${GADGETBRIDGE_DIR:-}}"

if [[ -z "${GB_ROOT}" ]]; then
  echo "Usage: $0 /path/to/Gadgetbridge" >&2
  echo "Or:    GADGETBRIDGE_DIR=/path/to/Gadgetbridge $0" >&2
  exit 1
fi

reject_unsafe_path "${GB_ROOT}" "Gadgetbridge path"
if [[ "${GB_ROOT}" != /* ]]; then
  echo "error: Gadgetbridge path must be absolute" >&2
  exit 1
fi

if [[ ! -d "${GB_ROOT}/app/src/main/java" ]]; then
  echo "error: ${GB_ROOT} does not look like a Gadgetbridge tree (missing app/src/main/java)" >&2
  exit 1
fi

GB_ROOT="$(realpath -e "${GB_ROOT}")"
reject_unsafe_path "${GB_ROOT}" "Gadgetbridge path"

JAVA_DEST="$(realpath -e "${GB_ROOT}/app/src/main/java")"
assert_inside "${JAVA_DEST}" "${GB_ROOT}" "app/src/main/java"

if [[ ! -f "${PATCH_FILE}" ]]; then
  echo "error: missing ${PATCH_FILE}" >&2
  exit 1
fi

verify_patch_paths
verify_source_tree

echo "Verifying patch applies cleanly to ${GB_ROOT} ..."
if git -C "${GB_ROOT}" rev-parse --git-dir >/dev/null 2>&1; then
  if ! git -C "${GB_ROOT}" apply --check "${PATCH_FILE}"; then
    echo "error: patch does not apply (conflicts with this Gadgetbridge revision?)." >&2
    echo "Regenerate integration/gadgetbridge-integration.patch or reset the two upstream files; see README." >&2
    exit 1
  fi
else
  if ! (cd "${GB_ROOT}" && patch -p1 --dry-run --forward < "${PATCH_FILE}" >/dev/null); then
    echo "error: patch dry-run failed for non-git tree." >&2
    exit 1
  fi
fi

echo "Copying add-on Java sources into ${JAVA_DEST} ..."
for pkg in "${ALLOWED_PACKAGES[@]}"; do
  src="${JAVA_SRC}/${pkg}"
  if [[ ! -d "${src}" ]]; then
    echo "error: missing ${src}" >&2
    exit 1
  fi
  dest="${JAVA_DEST}/${pkg}"
  mkdir -p "${dest}"
  dest="$(realpath -e "${dest}")"
  assert_inside "${dest}" "${JAVA_DEST}" "${pkg}"
  # --no-links: do not plant symlinks in the Gadgetbridge tree.
  rsync -a --no-links -- "${src}/" "${dest}/"
done

echo "Applying ${PATCH_FILE} ..."
if git -C "${GB_ROOT}" rev-parse --git-dir >/dev/null 2>&1; then
  git -C "${GB_ROOT}" apply "${PATCH_FILE}"
else
  if ! (cd "${GB_ROOT}" && patch -p1 --forward < "${PATCH_FILE}"); then
    echo "error: patch failed. Restore Java copies if needed (see README \"Remove\")." >&2
    exit 1
  fi
fi

echo "Done. Build with: (cd \"${GB_ROOT}\" && ./gradlew :app:assembleDebug)"

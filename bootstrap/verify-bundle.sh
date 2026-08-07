#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
xapi_root="$(cd -- "$script_dir/.." && pwd -P)"
manifest="$xapi_root/.xapi-bootstrap/MANIFEST.sha256"

if [[ ! -f "$manifest" ]]; then
  echo "Missing installed bootstrap manifest: $manifest" >&2
  echo "Extract xapi-bootstrap-seed-0.5.1.zip into $xapi_root first." >&2
  exit 2
fi

(
  cd "$xapi_root"
  sha256sum --check .xapi-bootstrap/MANIFEST.sha256
)

printf 'Bootstrap seed verified for %s\n' "$xapi_root"

#!/usr/bin/env bash
set -euo pipefail

readonly bootstrap_version="0.5.1"
readonly expected_gradle_sha="7d56671aaa23ef142211cdabd85eb3e6b9f9dac126dfb4503364ec69170bc64d"

script_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd -P)"
xapi_root="$(cd -- "$script_dir/.." && pwd -P)"
legacy_gradle_zip="${1:-${XAPI_LEGACY_GRADLE_ZIP:-/opt/gradle/gradle-5.1-x-24.zip}}"
output_zip="${2:-$xapi_root/build/bootstrap/xapi-bootstrap-seed-$bootstrap_version.zip}"
wti_seed_repo="${XAPI_WTI_SEED_REPO:-/opt/wti/repo}"
wti_shared_dir="$wti_seed_repo/de/mocra/cy/wti-shared"

if [[ ! -f "$legacy_gradle_zip" ]]; then
  echo "Missing customized Gradle distribution: $legacy_gradle_zip" >&2
  exit 2
fi
if [[ ! -d "$xapi_root/repoMvn" ]]; then
  echo "Missing bootstrap Maven seed: $xapi_root/repoMvn" >&2
  exit 3
fi
if [[ ! -f "$xapi_root/repoMvn/net/wetheinter/gwt-dev/2.8.0/gwt-dev-2.8.0.jar" ]]; then
  echo "repoMvn does not contain the expected customized gwt-dev seed" >&2
  exit 4
fi
if [[ ! -f "$wti_shared_dir/0.5.1-SNAPSHOT/maven-metadata.xml" ]]; then
  echo "Missing legacy wti-shared seed below: $wti_shared_dir" >&2
  echo "Set XAPI_WTI_SEED_REPO to the Maven repository containing it." >&2
  exit 6
fi

actual_gradle_sha="$(sha256sum "$legacy_gradle_zip" | awk '{print $1}')"
if [[ "$actual_gradle_sha" != "$expected_gradle_sha" ]]; then
  echo "Unexpected customized Gradle checksum: $actual_gradle_sha" >&2
  echo "Expected x-24 checksum: $expected_gradle_sha" >&2
  exit 5
fi

stage_dir="$(mktemp -d "${TMPDIR:-/tmp}/xapi-bootstrap.XXXXXX")"
cleanup() {
  if [[ "$stage_dir" == "${TMPDIR:-/tmp}/xapi-bootstrap."* && -d "$stage_dir" ]]; then
    rm -rf -- "$stage_dir"
  fi
}
trap cleanup EXIT

mkdir -p "$stage_dir/.xapi-bootstrap/gradle"
cp -p "$legacy_gradle_zip" "$stage_dir/.xapi-bootstrap/gradle/gradle-5.1-x-24.zip"
cp -p "$script_dir/README.md" "$stage_dir/.xapi-bootstrap/README.md"
cp -a "$xapi_root/repoMvn" "$stage_dir/repoMvn"
mkdir -p "$stage_dir/repoMvn/de/mocra/cy"
cp -a "$wti_shared_dir" "$stage_dir/repoMvn/de/mocra/cy/wti-shared"

created_utc="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"
{
  printf 'bundle.version=%s\n' "$bootstrap_version"
  printf 'bundle.createdUtc=%s\n' "$created_utc"
  printf 'gradle.file=%s\n' '.xapi-bootstrap/gradle/gradle-5.1-x-24.zip'
  printf 'gradle.sha256=%s\n' "$expected_gradle_sha"
  printf 'repoMvn.provenance=%s\n' 'local customized GWT/compiler seed; source provenance incomplete'
  printf 'wtiShared.provenance=%s\n' 'legacy /opt/wti sample dependency; source provenance incomplete'
} > "$stage_dir/.xapi-bootstrap/MANIFEST.properties"

(
  cd "$stage_dir"
  find .xapi-bootstrap repoMvn -type f ! -name MANIFEST.sha256 -print0 |
    LC_ALL=C sort -z |
    xargs -0 sha256sum > .xapi-bootstrap/MANIFEST.sha256
  sha256sum --check .xapi-bootstrap/MANIFEST.sha256 >/dev/null
)

output_dir="$(dirname -- "$output_zip")"
output_name="$(basename -- "$output_zip")"
mkdir -p "$output_dir"
temporary_zip="$(mktemp --suffix=.zip "$output_dir/.${output_name}.XXXXXX")"
rm -f -- "$temporary_zip"
(
  cd "$stage_dir"
  zip -q -0 -r "$temporary_zip" .xapi-bootstrap repoMvn
)
mv -f -- "$temporary_zip" "$output_zip"
(
  cd "$output_dir"
  sha256sum "$output_name" > "$output_name.sha256"
)

printf 'Created %s\n' "$output_zip"
du -h "$output_zip"
printf 'Archive SHA-256: '
sha256sum "$output_zip" | awk '{print $1}'

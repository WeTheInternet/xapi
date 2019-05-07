#!/usr/bin/env bash
set -e
args=$(
 (($# == 0)) && echo "build xapiPublish" || echo "$@"
)

pushd net.wti.gradle.tools > /dev/null
# the tools will install themselves to local repo whenever we build them.
./gradlew $args -x test
popd > /dev/null

shadow=${shadow:-"-x shadowJar"}
./gradlew $args -Dxapi.composite=true -Pxapi.changing=true -x test --parallel --build-cache -Pxapi.debug=false $shadow

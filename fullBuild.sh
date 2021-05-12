#!/usr/bin/env bash
set -e
args=$(
 (($# == 0)) && echo "build xapiPublish testClasses" || echo "$@"
)

function do_it() {

pushd net.wti.gradle.tools > /dev/null
# the tools will install themselves to local repo whenever we build them.
./gradlew $args -x test
popd > /dev/null


pushd net.wti.core > /dev/null
./gradlew $args -x test
popd > /dev/null


pushd net.wti.gradle > /dev/null
./gradlew $args -x test
popd > /dev/null


shadow=${shadow:-"-x shadowJar"}
./gradlew $args -Dxapi.composite=true -Pxapi.changing=true -x test -x check --parallel --build-cache -Pxapi.debug=false $shadow

}
TIMEFORMAT='Full build time: %3Rs'
time do_it

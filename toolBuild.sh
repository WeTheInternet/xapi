#!/usr/bin/env bash
set -e

do_all=y
[[ $1 == "--all" ]] && shift || do_all=n

args=$(
 (($# == 0)) && echo "build xapiPublish" || echo "$@"
)

pushd net.wti.gradle.tools > /dev/null
# the tools will install themselves to local repo whenever we build them.
./gradlew $args -x test
popd > /dev/null

if [ "$do_all" == y ]; then
    pushd net.wti.core > /dev/null
    ./gradlew $args -x test
    popd > /dev/null

    pushd net.wti.gradle > /dev/null
    ./gradlew $args -x test
    popd > /dev/null
fi

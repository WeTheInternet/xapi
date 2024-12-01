#!/bin/sh

./gradlew build -Dxapi.composite=false -x test -x check --parallel --build-cache -Pxapi.debug=false $@

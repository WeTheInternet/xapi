#!/bin/sh

#mvn install -T 2.5C -Dxapi.debug=false -Dxapi.log.level=WARN -DskipTests -Dxapi.build.quick=true $@

./gradlew build -Dxapi.composite=false -x test -x check --parallel --build-cache -Pxapi.debug=false $@

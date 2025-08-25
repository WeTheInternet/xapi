#!/bin/sh

./gradlew build -Dxapi.composite=false -x test -x check -x javadoc --parallel --build-cache -Pxapi.debug=false $@

#!/bin/sh

mvn install -T 2.5C -Dxapi.debug=false -Dxapi.log.level=WARN -DskipTests -Dxapi.build.quick=true $@

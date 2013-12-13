#!/bin/sh

mvn install -f settings/pom.xml -T 2.5C -Dxapi.log.level=WARN -Dxapi.build.quick=true $@

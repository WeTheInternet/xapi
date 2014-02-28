#!/bin/sh

mvn install -T 2.5C -Dxapi.log.level=WARN -Dxapi.build.quick=true $@

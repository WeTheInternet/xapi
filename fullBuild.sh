#!/bin/sh

mvn install -f settings/root.xml -T 2.5C -Dxapi.log.level=INFO -Dxapi.release=true -Dxapi.skip.test=false $@

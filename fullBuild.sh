#!/bin/sh

mvn clean install -T 2.5C -Dxapi.log.level=INFO -Dxapi.prod=true -Dxapi.debug=false -Dxapi.release=true -Dxapi.skip.test=false $@

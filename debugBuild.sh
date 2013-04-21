#!/bin/sh

mvn clean install -f settings/root.xml -Dxapi.debug=true -Dxapi.log.level=DEBUG -Dxapi.release=true -Dxapi.skip.test=false $@

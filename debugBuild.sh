#!/bin/sh

mvn clean install -Dxapi.debug=true -Dxapi.log.level=DEBUG -Dxapi.release=true -Dxapi.skip.test=false $@

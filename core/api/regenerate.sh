#!/bin/sh

mvn clean install -f ../../dev/source/pom.xml && mvn clean install -Dxapi.log.level=DEBUG "$@"

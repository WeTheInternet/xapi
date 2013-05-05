#!/bin/sh

mvn install -f ../../maven/pom.xml && mvn xapi:modelgen -Dxapi.log.level=DEBUG -T 2C "$@"

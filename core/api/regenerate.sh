#!/bin/sh


CLEAN=""
if [ "$1" = "clean" ]; then
  CLEAN="clean"
  shift
fi
clear

mvn $CLEAN install -f ../../pom.xml -pl net.wetheinter:xapi-dev-maven -am -T 2C
# Installs maven plugin, launched without blocking; run -o for offline; ~3 seconds
mvn $CLEAN install -o -f ../../maven/pom.xml 
# builds 15/70 modules in parallel, runs in ~6 seconds
# run the actual plugin; pass all args except clean to plugin
mvn -Dxapi.log.level=DEBUG -Dtarget.project=net.wetheinter:xapi-core-util -o xapi:annogen -e "$@"

#!/bin/sh

mvn install -f settings/root.xml -T 2.5C -Dxapi.log.level=WARN $@

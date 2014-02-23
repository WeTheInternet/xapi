#!/bin/sh

mvn release:prepare -Darguments="-Dsource.forceCreation=true -Dsource.includePom=true -Dxapi.skip.test=true -Dxapi.release=true" && mvn release:perform -Darguments="-Dsource.forceCreation=true -Dsource.includePom=true -Dxapi.skip.test=true -Dxapi.release=true" -Dgoals=deploy


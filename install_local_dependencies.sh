#!/bin/bash

mvn install:install-file -Dfile=lib/dbhelper-1.0.jar -DgroupId=fr.zenexity -DartifactId=dbhelper -Dversion=1.0 -Dpackaging=jar

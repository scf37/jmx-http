#!/bin/bash

ls -l "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0-SNAPSHOT.jar

PROJECT_VERSION=$(mvn help:evaluate -o -Dexpression=project.version | egrep -v '^\[|Downloading:')
#jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-${PROJECT_VERSION}.jar
jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0-SNAPSHOT.jar
# jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0-SNAPSHOT.jar -J-Xdebug -J-Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y

#!/bin/bash

PROJECT_VERSION=$(mvn help:evaluate -o -Dexpression=project.version | egrep -v '^\[|Downloading:')
echo "$PWD/$(dirname "$0")"/target/jmx-http-client-${PROJECT_VERSION}.jar
#jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-${PROJECT_VERSION}.jar
HOME=/home/asm/dev/jdk1.8.0_181/lib
 jconsole -J-Djava.class.path=$HOME/jconsole.jar:$HOME/tools.jar:./target/jmx-http-client-1.0.0-SNAPSHOT.jar
 #jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0-SNAPSHOT.jar
# jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0-SNAPSHOT.jar -J-Xdebug -J-Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y

#!/bin/bash

VERSION=1.0.0

CACHE_ROOT=~/.cache/jmx-http/$VERSION

mkdir -p $CACHE_ROOT

ARCHIVE_FILE=$CACHE_ROOT/jmx-http-client-$VERSION.jar

if [ ! -e "$ARCHIVE_FILE" ]; then
    echo "Downloading binaries..."
    curl -L https://repo.maven.apache.org/maven2/me/scf37/jmx-http/jmx-http-client/$VERSION/jmx-http-client-$VERSION.jar -o $ARCHIVE_FILE
fi


jvisualvm --cp:p $ARCHIVE_FILE &
# jvisualvm --cp:p "$PWD/$(dirname "$0")"/target/jmx-http-client-1.0.0.jar -J-Xdebug -J-Xrunjdwp:server=y,transport=dt_socket,address=8000,suspend=y

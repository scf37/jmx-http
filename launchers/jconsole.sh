#!/bin/bash

VERSION=1.0.0

CACHE_ROOT=~/.cache/jmx-http/$VERSION

mkdir -p $CACHE_ROOT

ARCHIVE_FILE=$CACHE_ROOT/jmx-http-client-$VERSION.jar

if [ ! -e "$ARCHIVE_FILE" ]; then
    echo "Downloading binaries..."
    curl -L https://dl.bintray.com/scf37/maven/me/scf37/jmx-http-client/$VERSION/jmx-http-client-$VERSION.jar -o $ARCHIVE_FILE
fi

jconsole -J-Xbootclasspath/a:$ARCHIVE_FILE

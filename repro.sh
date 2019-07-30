#!/bin/sh

# Usage: JAVA=./path/to/java repro.sh -i input_file [options]

JAVA=${JAVA:=java}
echo Using: $JAVA

cd "$(dirname "$0")"

$JAVA -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -cp target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar confuzzion.Repro ${@:2}

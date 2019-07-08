#!/bin/sh

# Usage: repro.sh ./path/to/java -i input_file

LOCAL_JAVA=$1

cd "$(dirname "$0")"

$LOCAL_JAVA -Dorg.slf4j.simpleLogger.defaultLogLevel=DEBUG -cp target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar confuzzion.Repro ${@:2}

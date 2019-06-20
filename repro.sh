#!/bin/sh

# Usage: repro.sh ./path/to/java -i input_file [-s]

LOCAL_JAVA=$1

cd "$(dirname "$0")"

$LOCAL_JAVA -cp target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar com.github.aztorius.confuzzion.Repro $@

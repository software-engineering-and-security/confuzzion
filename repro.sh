#!/bin/sh

# Usage: repro.sh ./path/to/class/TestX.class

LOCAL_PATH=$(dirname $1)
LOCAL_FILENAME=$(basename $1)
LOCAL_CLASSNAME=$(basename $LOCAL_FILENAME .class)

java -cp target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar:$LOCAL_PATH com.github.aztorius.confuzzion.Repro $LOCAL_CLASSNAME

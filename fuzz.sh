#!/bin/sh

# Usage: JAVA=./path/to/java ./fuzz.sh -t target.Class1:target.Class2 [options]

JAVA=${JAVA:=java}
echo Using: $JAVA

$JAVA -Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR -jar ./target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar $@

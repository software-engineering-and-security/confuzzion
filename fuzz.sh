#!/bin/sh

java -Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR -jar ./target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar $@

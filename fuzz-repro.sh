#!/bin/sh

./jqf/bin/jqf-repro -c ./target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar:./target/test-classes/ confuzziontest.ConfuzzionLauncher fuzz $1

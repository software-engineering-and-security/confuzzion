#!/bin/sh

echo "===Confuzzion fuzzing with Zest==="

mkdir -p /dev/shm/fuzz-results

ln -s /dev/shm/fuzz-results ./

./jqf/bin/jqf-ei -c ./target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar:./target/test-classes/:$(./jqf/scripts/classpath.sh) confuzziontest.ConfuzzionLauncher fuzz

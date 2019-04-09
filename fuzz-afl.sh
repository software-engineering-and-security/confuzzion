#!/bin/sh

echo "===Confuzzion fuzzing with AFL==="

mkdir -p /dev/shm/fuzz-results

ln -s /dev/shm/fuzz-results ./

mkdir -p ./fuzz-results/out/

./jqf/bin/jqf-afl-fuzz -c ./target/confuzzion-1.0-SNAPSHOT-jar-with-dependencies.jar:./target/test-classes/ -o ./fuzz-results/out/ -v confuzziontest.ConfuzzionLauncher fuzz

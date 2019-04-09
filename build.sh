#!/bin/sh

echo "===Building Confuzzion==="
mvn clean test-compile package

echo "===Building JQF==="
cp ./janala.conf ./jqf/scripts/
./jqf/setup.sh

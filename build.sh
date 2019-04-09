#!/bin/sh

echo "===Building Confuzzion==="
mvn clean test-compile package

echo "===Building JQF==="
./jqf/setup.sh

#!/bin/sh

mkdir /dev/shm/fuzz-results

ln -s /dev/shm/fuzz-results target/fuzz-results

mvn jqf:fuzz -Dclass=confuzziontest.ConfuzzionLauncher -Dmethod=fuzz

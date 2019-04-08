#!/bin/sh

mkdir /dev/shm/fuzz-results

ln -s /dev/shm/fuzz-results target/

mvn jqf:fuzz -Dclass=confuzziontest.ConfuzzionLauncher -Dmethod=fuzz -Dincludes=java.io.ByteArrayOutputStream

#-Dblind for no mutation, just random inputs

#mvn jqf:repro -Dclass=confuzziontest.ConfuzzionLauncher -Dmethod=fuzz -DlogCoverage=coverage.out -Dinput=target/fuzz-results/confuzziontest.ConfuzzionLauncher/fuzz/corpus/id_000000


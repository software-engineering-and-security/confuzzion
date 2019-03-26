# Confuzzion

## Build

mvn test-compile

## Package

mvn package

## Test

mvn exec:java

or

mvn jqf:fuzz -Dclass=confuzziontest.ConfuzzionLauncher -Dmethod=fuzz

## Usage

java -jar confuzzion.jar

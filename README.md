# Confuzzion

## Build

mvn test-compile

## Package

mvn package

## Test

mvn exec:java

## Usage

To use mutation algorithm with 10 rounds for main and constant loops :
`./fuzz.sh mut -m 10 -c 10`

Tu use mutation algorithm with infinite rounds :
`./fuzz.sh mut -m -1 -c -1`

Tu use generation algorithm with infinite rounds :
`./fuzz.sh gen -m -1`

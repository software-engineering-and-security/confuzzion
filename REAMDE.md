# Confuzzion

## Build

mvn test-compile

## Package

mvn package

## Test

mvn exec:java

## Usage

To use mutation algorithm with 10 rounds for main and constant loops :
`./fuzz.sh mutate 10 10`

Tu use mutation algorithm with infinite rounds :
`./fuzz.sh mutate -1 -1`

Tu use generation algorithm with infinite rounds :
`./fuzz.sh generate -1`

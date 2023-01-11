#!/bin/sh

# Find possible target classes inside openjdk8

VERSION_FROM=jdk8u222-ga
VERSION_TO=jdk8u232-ga

if [ ! -d ./jdk8u/ ]; then
  # Clone openjdk8 Mercurial repository first
  hg clone https://hg.openjdk.java.net/jdk8u/jdk8u/
fi

cd jdk8u/

if [ ! -d ./jdk/ ]; then
  chmod u+x ./get_source.sh
  ./get_source.sh
fi

cd jdk/

hg diff -r $VERSION_TO -r $VERSION_FROM src/share/classes/ | grep diff | cut -d " " -f 6

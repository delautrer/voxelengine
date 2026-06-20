#!/bin/bash
cd "$(dirname "$0")"

JVM_ARGS="--enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8"

if [[ "$OSTYPE" == "darwin"* ]]; then
    JVM_ARGS="$JVM_ARGS -XstartOnFirstThread"
fi

java $JVM_ARGS -jar build/libs/Veinstride-Obfuscated.jar

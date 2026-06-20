#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
exec "$DIR/../PlugIns/jre/Contents/Home/bin/java" -XstartOnFirstThread -Xdock:name="Veinstride" --enable-native-access=ALL-UNNAMED -XstartOnFirstThread -Dfile.encoding=UTF-8 -jar "$0" "$@"
exit 0

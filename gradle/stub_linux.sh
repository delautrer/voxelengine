#!/bin/bash
DIR="$(cd "$(dirname "$0")" && pwd)"
cd "$DIR"
unset WAYLAND_DISPLAY
export XDG_SESSION_TYPE=x11
exec "$DIR/jre/bin/java" --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar "$0" "$@"
exit 0

#!/usr/bin/env bash
# Compiles CQL's main sources with javac alone, against the jars in lib/, as Eclipse users build
# it, so that nothing is downloaded. Usage: compile_with_javac.sh <output directory>
set -euo pipefail

out="$1"
sources="$(mktemp)"
mkdir -p "$out"
find src -name '*.java' \
  ! -name '*Test.java' \
  ! -name '*TestHelpers.java' \
  ! -name 'OilExample.java' > "$sources"
javac -encoding UTF-8 -Xlint:none -d "$out" -cp "lib/*" "@$sources"
rm "$sources"

#!/usr/bin/env bash
set -euo pipefail
root="$(cd "$(dirname "$0")" && pwd)"
classes="$(mktemp -d)"
trap 'rm -rf "$classes"' EXIT
javac_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}javac"
java_cmd="${JAVA_HOME:+$JAVA_HOME/bin/}java"
"$javac_cmd" -d "$classes" \
    "$root/app/src/main/java/org/supermariowar/app/TouchInput.java" \
    "$root/app/src/main/java/org/supermariowar/app/TouchLayout.java" \
    "$root/tests/TouchControlsTest.java"
"$java_cmd" -cp "$classes" org.supermariowar.app.TouchControlsTest

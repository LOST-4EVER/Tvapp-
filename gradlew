#!/usr/bin/env sh
# Gradle wrapper script
set -e

DIRNAME="$(cd "$(dirname "$0")" && pwd)"

if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

if [ -f "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" ]; then
    JAVACMD="${JAVA_HOME}/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        JAVACMD="java"
    fi
    exec "$JAVACMD" -classpath "$DIRNAME/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
fi

echo "Error: Neither Gradle CLI nor gradle-wrapper.jar was found." >&2
exit 1

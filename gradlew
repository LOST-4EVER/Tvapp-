#!/usr/bin/env sh
#
# Gradle launcher for this project.
#
# The standard Gradle wrapper needs gradle/wrapper/gradle-wrapper.jar, which is a
# binary that is not tracked here. This script is the replacement, and it has
# three fallbacks rather than one:
#
#   1. A Gradle already on PATH is used as-is.
#   2. Otherwise, if gradle/wrapper/gradle-wrapper.jar is present, the official
#      wrapper is used.
#   3. Otherwise the distribution named by
#      gradle/wrapper/gradle-wrapper.properties is downloaded once, cached under
#      the Gradle user home, and used directly.
#
# The point of (3) is that a clean checkout plus a JDK is enough to build. The
# version that gets used still comes from the same properties file the official
# wrapper reads, so it cannot drift from what CI uses.

set -e

DIRNAME=$(cd "$(dirname "$0")" >/dev/null 2>&1 && pwd)
WRAPPER_PROPS="$DIRNAME/gradle/wrapper/gradle-wrapper.properties"
WRAPPER_JAR="$DIRNAME/gradle/wrapper/gradle-wrapper.jar"

die() {
    echo "ERROR: $*" >&2
    exit 1
}

# Locate a JDK. Gradle 9 needs a Java 17+ runtime; the build itself sets 21.
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/java" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
elif command -v java >/dev/null 2>&1; then
    JAVACMD=$(command -v java)
else
    die "No Java runtime found. Install JDK 17 or newer, or set JAVA_HOME."
fi

# 1. A Gradle on PATH wins, so a developer who has one keeps using it.
if command -v gradle >/dev/null 2>&1; then
    exec gradle "$@"
fi

# 2. The official wrapper, when the jar has been restored into the checkout.
if [ -f "$WRAPPER_JAR" ]; then
    exec "$JAVACMD" $JAVA_OPTS -classpath "$WRAPPER_JAR" org.gradle.wrapper.GradleWrapperMain "$@"
fi

# 3. Download the pinned distribution ourselves.
[ -f "$WRAPPER_PROPS" ] || die "Neither a Gradle install nor $WRAPPER_PROPS was found."

DIST_URL=$(sed -n 's/^distributionUrl=//p' "$WRAPPER_PROPS" | tr -d '\r' | sed 's/\\//g')
[ -n "$DIST_URL" ] || die "Could not read distributionUrl from $WRAPPER_PROPS."

# The zip always unpacks to a single <name>-<version> directory; derive the
# install path from it rather than trying to re-parse the version separately.
DIST_ZIP="${DIST_URL##*/}"
DIST_NAME="${DIST_ZIP%-bin.zip}"
DIST_NAME="${DIST_NAME%-all.zip}"

if [ -z "$GRADLE_USER_HOME" ]; then
    GRADLE_USER_HOME="$HOME/.gradle"
fi
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/$DIST_NAME"
GRADLE_BIN="$DIST_DIR/$DIST_NAME/bin/gradle"

if [ ! -x "$GRADLE_BIN" ]; then
    echo "Downloading Gradle from $DIST_URL" >&2
    mkdir -p "$DIST_DIR"
    ZIP_PATH="$DIST_DIR/$DIST_ZIP"

    if command -v curl >/dev/null 2>&1; then
        curl -fsSL -o "$ZIP_PATH" "$DIST_URL" \
            || die "Download failed. Check the network, or install Gradle and re-run."
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$ZIP_PATH" "$DIST_URL" \
            || die "Download failed. Check the network, or install Gradle and re-run."
    else
        die "Need curl or wget to download Gradle, or a Gradle already on PATH."
    fi

    # unzip when available, otherwise the JDK's own jar tool, which is always
    # present because we have already established that java exists.
    if command -v unzip >/dev/null 2>&1; then
        unzip -q -o "$ZIP_PATH" -d "$DIST_DIR"
    else
        if command -v jar >/dev/null 2>&1; then
            (cd "$DIST_DIR" && jar xf "$ZIP_PATH")
        else
            die "Need unzip (or a JDK providing jar) to unpack Gradle."
        fi
    fi

    rm -f "$ZIP_PATH"
    chmod +x "$GRADLE_BIN" 2>/dev/null || true

    [ -x "$GRADLE_BIN" ] || die "Gradle unpacked to an unexpected layout under $DIST_DIR."
    echo "Gradle cached at $GRADLE_BIN" >&2
fi

exec "$GRADLE_BIN" "$@"

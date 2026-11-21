#!/usr/bin/env sh
# Pinned Gradle bootstrap. Downloads the exact Gradle distribution if not cached.
set -e
GRADLE_VERSION=9.6.0
if [ -z "$GRADLE_USER_HOME" ]; then GRADLE_USER_HOME="$HOME/.gradle"; fi
DIST_DIR="$GRADLE_USER_HOME/dailyfit/gradle-$GRADLE_VERSION"
GRADLE_BIN="$DIST_DIR/gradle-$GRADLE_VERSION/bin/gradle"
if [ ! -x "$GRADLE_BIN" ]; then
  mkdir -p "$DIST_DIR"
  ZIP="$DIST_DIR/gradle-$GRADLE_VERSION-bin.zip"
  if [ ! -f "$ZIP" ]; then curl -fL "https://services.gradle.org/distributions/gradle-$GRADLE_VERSION-bin.zip" -o "$ZIP"; fi
  unzip -q -o "$ZIP" -d "$DIST_DIR"
fi
exec "$GRADLE_BIN" "$@"

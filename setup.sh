#!/bin/bash
set -e

# This script prepares an offline Gradle cache with the IntelliJ dependencies
# required to build the project. It downloads the IntelliJ IDEA binaries and
# the IntelliJ Gradle plugin then runs the build in offline mode.
#
# If downloads fail due to network restrictions, manually place the required
# files in the local-repo directory before running the build.

INTELLIJ_VERSION=2023.1
INTELLIJ_URL="https://cache-redirector.jetbrains.com/intellij-repository/releases/com/jetbrains/intellij/idea/ideaIC/${INTELLIJ_VERSION}/ideaIC-${INTELLIJ_VERSION}.zip"

INTELLIJ_PLUGIN_VERSION=1.17.3
INTELLIJ_PLUGIN_URL="https://plugins.gradle.org/m2/org/jetbrains/intellij/org.jetbrains.intellij.gradle.plugin/${INTELLIJ_PLUGIN_VERSION}/org.jetbrains.intellij.gradle.plugin-${INTELLIJ_PLUGIN_VERSION}.jar"

LOCAL_REPO="local-repo"
mkdir -p "$LOCAL_REPO"

if [ ! -f "$LOCAL_REPO/ideaIC-${INTELLIJ_VERSION}.zip" ]; then
  echo "Downloading IntelliJ IDEA ${INTELLIJ_VERSION}..."
  curl -L -o "$LOCAL_REPO/ideaIC-${INTELLIJ_VERSION}.zip" "$INTELLIJ_URL"
fi

if [ ! -f "$LOCAL_REPO/org.jetbrains.intellij.gradle.plugin-${INTELLIJ_PLUGIN_VERSION}.jar" ]; then
  echo "Downloading IntelliJ Gradle plugin ${INTELLIJ_PLUGIN_VERSION}..."
  curl -L -o "$LOCAL_REPO/org.jetbrains.intellij.gradle.plugin-${INTELLIJ_PLUGIN_VERSION}.jar" "$INTELLIJ_PLUGIN_URL"
fi

echo "Running Gradle build in offline mode..."
gradle --offline -Dmaven.repo.local="$LOCAL_REPO" build

#!/usr/bin/env bash
# Runs the JavaFX client via the javafx-maven-plugin.
set -e

cd "$(dirname "$0")"
mvn -q -pl client -am javafx:run

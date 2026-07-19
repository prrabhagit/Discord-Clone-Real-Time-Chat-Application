#!/usr/bin/env bash
# Builds (if needed) and runs the ChatApp server.
# Usage: ./run-server.sh [path/to/config.properties]
set -e

cd "$(dirname "$0")"

if [ ! -f server/target/chatapp-server.jar ]; then
    echo "Building project (mvn clean package)..."
    mvn -q clean package -DskipTests
fi

if [ -n "$1" ]; then
    java -Dchatapp.config="$1" -jar server/target/chatapp-server.jar
else
    java -jar server/target/chatapp-server.jar
fi

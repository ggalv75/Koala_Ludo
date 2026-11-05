#!/usr/bin/env bash
# Builds LudoKoala.jar from the src folder (requires JDK 17 or newer).
set -euo pipefail
cd "$(dirname "$0")"
rm -rf build
mkdir -p build/classes
find src -name '*.java' > build/sources.txt
javac --release 17 -encoding UTF-8 -d build/classes @build/sources.txt
jar --create --file LudoKoala.jar --main-class Controller.LudoApplication -C build/classes .
echo "Built LudoKoala.jar"

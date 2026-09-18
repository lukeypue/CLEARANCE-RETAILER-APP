#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
test_output_dir=$(mktemp -d)
trap 'rm -rf "$test_output_dir"' EXIT
mapfile -t sources < <(find app/src/main/java/com/clearance/retailer/model app/src/main/java/com/clearance/retailer/domain -name '*.java' -type f)
java -m jdk.compiler/com.sun.tools.javac.Main -encoding UTF-8 -d "$test_output_dir" "${sources[@]}" app/src/main/java/com/clearance/retailer/data/DemoCatalog.java tests/DomainTests.java tests/NearbyTests.java
java -cp "$test_output_dir" DomainTests
java -cp "$test_output_dir" NearbyTests

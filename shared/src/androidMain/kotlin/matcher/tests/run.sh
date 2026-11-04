#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="${MATCHER_HOST_BUILD_DIR:-/tmp/valera-matcher-host-tests}"
mkdir -p "$output"
gcc -std=c11 -O2 -I../matcher_common -c ../matcher_common/cJSON.c -o "$output/cJSON.o"
g++ '-D__has_feature(x)=0' -std=c++20 -O2 -Wall -Wextra -Wno-unused-parameter -ffunction-sections -fdata-sections \
    -I. -I../matcher_common tests/typed_claims_test.cpp CredentialDatabase.cpp dcql.cpp \
    cppbor.cpp cppbor_parse.cpp logger.cpp paths.cpp "$output/cJSON.o" \
    -Wl,--gc-sections -o "$output/typed-claims-test"
"$output/typed-claims-test" "$@"

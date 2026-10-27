#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")/.."
output="${MATCHER_HOST_BUILD_DIR:-/tmp/valera-matcher-host-tests}"
mkdir -p "$output"
gcc -std=c11 -O2 -I../matcher_common -c ../matcher_common/cJSON.c -o "$output/cJSON.o"
build() {
    g++ '-D__has_feature(x)=0' -std=c++20 -O2 -Wall -Wextra -Wno-unused-parameter -ffunction-sections -fdata-sections \
        -I. -I../matcher_common "$1" CredentialDatabase.cpp dcql.cpp "${@:3}" \
        cppbor.cpp cppbor_parse.cpp logger.cpp paths.cpp "$output/cJSON.o" \
        -Wl,--gc-sections -o "$output/$2"
}
build tests/typed_claims_test.cpp typed-claims-test
build tests/iso_sdjwt_test.cpp iso-sdjwt-test Request.cpp
if [[ $# -gt 0 ]]; then
    "$output/typed-claims-test" "$@"
else
    "$output/typed-claims-test"
    "$output/iso-sdjwt-test"
fi

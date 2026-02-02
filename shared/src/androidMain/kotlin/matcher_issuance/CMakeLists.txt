# Copyright (c) 2025.
# SPDX-License-Identifier: Apache-2.0
# Source: Derived from CMWallet and adapted for this project.

cmake_minimum_required(VERSION 3.30)
project(Matcher)

# Add include directories
include_directories(. ../matcher_common)

# Common library source files
set(COMMON_LIB_SRCS
    ../matcher_common/cJSON.c
    credentialmanager.c
)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Target: issuance_provision.wasm
add_library(issuance_provision issuance/provision.c dcql.c ${COMMON_LIB_SRCS})
set_target_properties(issuance_provision PROPERTIES SUFFIX ".wasm")

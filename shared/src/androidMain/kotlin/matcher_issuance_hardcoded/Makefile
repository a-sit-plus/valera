# Build the provision_hardcoded matcher to WebAssembly using wasi-sdk.
# Source: Derived from multipaz and adapted for this project.

WASM_CLANG = ~/wasi-sdk-20.0/bin/clang

C_SOURCES = issuance/provision_hardcoded.c credentialmanager.c ../matcher_common/cJSON.c
OBJECTS = build/provision_hardcoded.o build/credentialmanager.o build/cJSON.o

CFLAGS = -c -O3 -I. -I.. -I../matcher_common -Iissuance

all: build/dcapimatcher_issuing_hardcoded.wasm

build:
	mkdir -p $@

build/dcapimatcher_issuing_hardcoded.wasm: $(OBJECTS) build
	$(WASM_CLANG) -o $@ $(OBJECTS) -O3

copy: build/dcapimatcher_issuing_hardcoded.wasm
	cp build/dcapimatcher_issuing_hardcoded.wasm ../../../../../androidApp/src/androidMain/assets/dcapimatcher_issuing_hardcoded.wasm

build/provision_hardcoded.o: issuance/provision_hardcoded.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

build/credentialmanager.o: credentialmanager.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

build/cJSON.o: ../matcher_common/cJSON.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

clean:
	rm -rf build

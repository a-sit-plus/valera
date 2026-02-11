# Build the issuance_provision matcher to WebAssembly using wasi-sdk.

WASM_CLANG = ~/wasi-sdk-20.0/bin/clang

C_SOURCES = issuance/provision.c dcql.c credentialmanager.c ../matcher_common/cJSON.c
OBJECTS = build/provision.o build/dcql.o build/credentialmanager.o build/cJSON.o

CFLAGS = -c -O3 -I.. -I../matcher_common -I.

all: build/issuance_provision.wasm

build:
	mkdir -p $@

build/issuance_provision.wasm: $(OBJECTS) build
	$(WASM_CLANG) -o $@ $(OBJECTS) -O3

copy: build/issuance_provision.wasm
	cp build/issuance_provision.wasm ../../../../../androidApp/src/androidMain/assets/dcapimatcher_issuing.wasm

build/provision.o: issuance/provision.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

build/dcql.o: dcql.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

build/credentialmanager.o: credentialmanager.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

build/cJSON.o: ../matcher_common/cJSON.c build
	$(WASM_CLANG) -c -o $@ $< $(CFLAGS)

clean:
	rm -rf build

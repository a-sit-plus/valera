// based on identity-credential[https://github.com/openwallet-foundation-labs/identity-credential] implementation

extern "C" void matcher(void);

// This is the entrypoint used in the WASM binary.
extern "C" int main() {
    matcher();
    return 0;
}

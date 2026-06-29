This directory contains a hardcoded issuance matcher written in C based on the
[CMWallet](https://github.com/digitalcredentialsdev/CMWallet) implementation.

To compile it you need the [WASI SDK](https://github.com/WebAssembly/wasi-sdk/releases)
toolchain installed, specifically version 20. It should be installed in `~/wasi-sdk-20.0`.

Build and copy the matcher into the app assets:

```shell
$ make clean && make -j && make copy
```

This produces `build/dcapimatcher_issuing_hardcoded.wasm` and copies it to
`../../../../../androidApp/src/androidMain/assets/dcapimatcher_issuing_hardcoded.wasm`.

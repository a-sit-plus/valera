This directory contains a Credman matcher written in C based on the
[identity-credential](https://github.com/openwallet-foundation-labs/identity-credential)
implementation.

To compile it, you need the [WASI SDK](https://github.com/WebAssembly/wasi-sdk/releases)
toolchain installed, specifically version 20. It should be installed in `~/wasi-sdk-20.0`.

Build with CMake:

```shell
$ mkdir -p build && cmake -S . -B build && cmake --build build
```

This produces `build/issuance_provision.wasm`. Copy it into
`../../../../../androidApp/src/androidMain/assets/dcapimatcher_issuing.wasm` to register it:

```shell
$ cp build/libissuance_provision.wasm ../../../../../androidApp/src/androidMain/assets/dcapimatcher_issuing.wasm
```

To update the launcher icon, convert a PNG into the C header format used by
`issuance/launcher_icon.h` using the helper script:

```shell
$ python3 issuance/png_to_c_array.py issuing_icon.png issuance/launcher_icon.h _launcher_icon_png
```

The [cJSON library](https://github.com/DaveGamble/cJSON) is shared in
`../matcher_common/cJSON.[c, h]` with license in `../matcher_common/cJSON-LICENSE` file.

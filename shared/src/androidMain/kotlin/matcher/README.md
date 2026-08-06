

This directory contains a Credman matcher written in C/C++ based on the 
[identity-credential](https://github.com/openwallet-foundation-labs/identity-credential)
implementation.

To compile it you need the [WASI SDK](https://github.com/WebAssembly/wasi-sdk/releases)
toolchain installed, specifically version 20. It should be installed in `~/wasi-sdk-20.0`.

The bundled `Makefile` will build the `build/matcher.wasm` binary which can be copied
into `../assets/identitycredentialmatcher.wasm` where it will get picked up as part
of the identity-appsupport library. The following command-line does this

```shell
$ make clean && make -j && cp build/matcher.wasm ../../../../../androidApp/src/androidMain/assets/dcapimatcher.wasm
```

The [cJSON library](https://github.com/DaveGamble/cJSON) is shared in
`../matcher_common/cJSON.[c, h]` with license in `../matcher_common/cJSON-LICENSE` file.

The [LibCppBor library](https://android.googlesource.com/platform/system/libcppbor/) is
bundled as `cppbor.[cpp, h]` and `cppbor_parse.[cpp, h]`. This is licensed under the Apache
License, Version 2.0.

## Valera patches

Local changes that must survive a source refresh from the parent projects are stored in
the `patches` directory. After replacing the matcher sources with a newer upstream version,
apply each patch before rebuilding the WebAssembly asset. For example, from the repository
root run:

```shell
git apply --check shared/src/androidMain/kotlin/matcher/patches/empty-claims-mandatory-attributes.patch
git apply shared/src/androidMain/kotlin/matcher/patches/empty-claims-mandatory-attributes.patch
cd shared/src/androidMain/kotlin/matcher
make clean && make -j
cp build/matcher.wasm ../../../../../androidApp/src/androidMain/assets/dcapimatcher.wasm
```

The `--check` command detects upstream conflicts without changing files. If it fails, port
the small change manually and refresh the patch before committing the updated matcher source
and generated asset together.

`empty-claims-mandatory-attributes.patch` adds a synthetic field to matching credentials when
a DCQL credential query omits `claims`, so Android's system picker can display that all
mandatory attributes are requested.

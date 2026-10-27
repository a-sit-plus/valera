

This directory contains a Credman matcher written in C/C++ based on the
[Multipaz matcher](https://github.com/openwallet-foundation/multipaz/tree/main/multipaz-dcapi-credman/src/androidMain/matcher).
The source was last synchronized with upstream commit
[`f7d8e684a3d32bfd498c07742bfc0bc1b75b0bb9`](https://github.com/openwallet-foundation/multipaz/commit/f7d8e684a3d32bfd498c07742bfc0bc1b75b0bb9).

To compile it you need the [WASI SDK](https://github.com/WebAssembly/wasi-sdk/releases)
toolchain installed, specifically version 20. It should be installed in `~/wasi-sdk-20.0`.

The bundled `Makefile` will build the `build/matcher.wasm` binary which can be copied
into `../assets/identitycredentialmatcher.wasm` where it will get picked up as part
of the identity-appsupport library. The following command-line does this

```shell
$ make clean && make -j && cp build/matcher.wasm ../../../../../androidApp/src/main/assets/dcapimatcher.wasm
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
for patch in shared/src/androidMain/kotlin/matcher/patches/*.patch; do
    git apply --check "$patch"
    git apply "$patch"
done
cd shared/src/androidMain/kotlin/matcher
make clean && make -j
cp build/matcher.wasm ../../../../../androidApp/src/main/assets/dcapimatcher.wasm
```

The `--check` command detects upstream conflicts without changing files. If it fails, port
the small change manually and refresh the patch before committing the updated matcher source
and generated asset together.

`empty-claims-mandatory-attributes.patch` adds a synthetic field to matching credentials when
a DCQL credential query omits `claims`, so Android's system picker can display that all
mandatory attributes are requested.

`shared-cjson-build.patch` builds the shared cJSON from `../matcher_common`, which the issuing
matcher uses as well, instead of a copy next to the sources.

`typed-sdjwt-iso-requests.patch` matches SD-JWT VCs requested in an ISO/IEC 18013-7 Annex C
device request as proposed by WG10 N2933 (experimental): a `DocRequest` with `docFormat`
`dc+sd-jwt` requests the SD-JWT VC of its `docType` (the `vct`), with each data element of the
namespace `_` mapped by `dataElementIdentifierMapping` to a claim path of text strings, unsigned
integers and `null`. The paths become typed claims path pointers, matched like those of a DCQL
query against the typed claims tree (see `typed-sdjwt-claims.patch`). Every requested claim is
required, or one of its `alternativeDataElements` sets, also in requests of version 1.0, unlike
the data elements of an mdoc. An element without a valid mapping, e.g. with a negative integer or
an empty path, or in another namespace, makes the request match no credential. Without requested
elements, the credential is offered for its mandatory claims only.

`show-all-protocols.patch` keeps all matching request protocols visible in the picker and adds
the selected protocol to each entry's subtitle. Upstream stops after the first matching protocol.

`disable-trusted-authority-reader-matching.patch` adds the top-level credential-database CBOR
boolean `enforceTrustedAuthorityAndReaderIdentifierMatching`. Matching is disabled when the field
is absent, malformed, or `false`; setting it to `true` enforces both trusted-authority issuer
identifiers and reader identifiers while preserving all other matching criteria.

`typed-sdjwt-claims.patch` replaces the obsolete flattened wildcard matching patch. SD-JWT
entries require `claimsFormatVersion: 1` and a recursive `claimsTree`: object `members` retain
literal names; array `elements` retain positions; scalar `kind` and untruncated `value` retain
JSON types. `displayName` and `displayValue` are display-only metadata. Empty objects, arrays
and JSON null remain real nodes. Missing/unsupported versions and malformed trees cannot match,
even when no claims are requested. There is no flattened-claims fallback.

DCQL evaluation keeps member, index and wildcard segments distinct and processes claims path
pointers as OpenID4VP 1.0, 7.1.1 specifies: if any selected element is not of the container type
the next segment requires, processing ends with an error, as it does when the selection becomes
empty. Absent members/indices drop branches. Such an error means that this claim does not match,
so another option of `claim_sets` that does not need it may still match the credential query. A
malformed path or value constraint makes the request invalid instead, so the credential query
matches no credential, whichever claim sets it offers. Value constraints require an exact
scalar type and value from at least one selected node. Length-prefixed typed cache keys and
immutable `std::map` results preserve separate query results and stable picker claim pointers.

Numeric request parsing uses cJSON's double representation. Indices must be nonnegative integral
numbers below 2^53 after cJSON parsing; negative and nonintegral parsed indices are rejected.
cJSON has already rounded the original numeric token, so a fractional lexical index such as
`0.99999999999999999` can appear integral (`1`) at this stage. Numeric constraints with
absolute value at least 2^53 are rejected rather than silently matching rounded large integers.
Safe integers are rendered from `int64_t`, avoiding both cJSON's 32-bit `valueint` saturation and
its shortened numeric printing. Stored numbers are compared by exact normalized decimal content
(`1.50` equals `1.5`), never by rounded doubles. Noninteger request values still inherit cJSON's
lexical precision limitation: the original numeric token is unavailable after parsing, so their
meaning is cJSON's printed decimal value. Exact arbitrary-precision request-number handling
and original-token index validation would require changing the shared JSON parser contract.
Stored decimal exponents outside the
bounded normalization range (roughly ±10 million) fail closed.

## Host regression checks

From the repository root, with installed GCC/G++:

```shell
bash shared/src/androidMain/kotlin/matcher/tests/run.sh
```

This builds under `/tmp/valera-matcher-host-tests` (override `MATCHER_HOST_BUILD_DIR` if needed)
and executes the real `CredentialDatabase` CBOR parser and `DcqlQuery::parse`/`execute` against
synthetic cppbor fixtures. It covers parent claims, nested wildcard/index paths, literal dotted,
numeric, null and empty names, wrong selected types with and without alternative claim sets,
malformed paths next to alternative claim sets, missing paths,
empty containers and JSON null, scalar types, long and empty values, safe-integer boundaries,
malformed trees/paths/values, distinct result caches and stable pointers, mdoc and query IDs.
A second binary, `iso-sdjwt-test`, parses Annex C device requests with `MdocRequest::parseMdocApi` and
covers SD-JWT document requests: typed paths, vct and namespace checks, invalid mappings, strict
matching in version 1.0, alternative sets, and requests without claims.
The host GCC invocation supplies the Clang `__has_feature` fallback used by bundled cppbor;
unused picker integration sections are discarded by the linker. This is not a WASM runtime test.

For a CBOR registry produced by the actual Kotlin exporter and a DCQL JSON object:

```shell
/tmp/valera-matcher-host-tests/typed-claims-test --fixture registry.cbor dcql.json
```

It prints `MATCH` (exit 0) or `NO MATCH` (exit 1), using `openid4vp-v1-unsigned` by default.
An optional final protocol argument overrides that default. Pass the DCQL object itself,
not an OpenID4VP request envelope. Rebuild and exercise the actual WASM asset separately after
source changes; host checks do not replace the bundled-asset requirement.

`report-credential-query-id.patch` appends the identifier of the DCQL credential query to each
entry ID, i.e. `<combination> <protocol> <documentId> <credentialQueryId>`, as the picker shows
the claims of that query. The wallet presents the selected credential for exactly that query.
Upstream reports only the document, which may match several queries, e.g. alternative options of
one credential set, so the wallet could not tell which claims the user agreed to share.

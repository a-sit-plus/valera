# AGENTS.md

## Project overview

Valera is a Kotlin/Compose Multiplatform identity wallet for Android and iOS, powered by VC-K. It supports credential issuance, storage, refresh, and presentation through OpenID4VCI, OpenID4VP, the Digital Credentials API, BLE, and NFC.

This is a technology demonstrator, not a production wallet. Never use or add real identity data, production secrets, signing credentials, or trust anchors.

Read `README.md`, `DEVELOPMENT.md`, and `CONTRIBUTING.md` before changing release, signing, entitlement, or deployment behavior.

## Repository layout

- `shared/`: shared business logic, Compose UI, Koin modules, persistence, and platform source sets.
  - `src/commonMain`: platform-independent code.
  - `src/androidMain`: Android actuals and integrations.
  - `src/iosMain` and `src/appleMain`: Apple actuals and integrations.
  - `src/commonTest`, `src/androidHostTest`, `src/androidDeviceTest`, `src/iosTest`: tests.
- `androidApp/`: Android application, activities, retained session host, manifest, resources, and DC API matcher assets. Plain Android Gradle module, so it uses the standard AGP layout (`src/main`, `src/debug`), not Kotlin Multiplatform source-set names.
- `iosApp/`: Swift/Xcode host app and Identity Document Provider extension.
- `interop/` and `cinterop/`: Apple Digital Credentials interop.
- `../vck`: Valera is a consumer of VC-K. When this sibling repository is present, Valera automatically includes it as a Gradle composite build instead of using only the published VC-K artifacts.
- `../signum`: VC-K in turn consumes Signum for cryptographic primitives and operations. When this sibling repository is present, the VC-K build automatically includes it as another composite build.

The effective source dependency chain can therefore be:

```text
Valera (this repository) -> ../vck -> ../signum
```

When those sibling checkouts exist, their source code is available for inspection and Gradle builds may compile it as part of this project. Trace defects to the layer that owns the behavior: application and platform integration generally belong here, credential/protocol behavior may belong in VC-K, and cryptographic behavior may belong in Signum. Changes may be made in `../vck` or `../signum` when the correct fix belongs there rather than adding an application-level workaround. Treat each sibling repository as an independent worktree: inspect its guidance and status, preserve unrelated changes, and verify the affected upstream project as well as Valera after cross-repository edits.

Follow official Kotlin style and the conventions already present in the surrounding files.

## Architecture and dependency injection

Koin has application-wide bindings and a named session scope (`SESSION_NAME`). Session construction is coordinated by `WalletSessionScopeFactory`, with `SessionService` responsible for closing the active session.

Keep session-owned objects inside the session scope. A session close must:

- invoke the session close callback;
- cancel its coroutine scope; and
- close its Koin scope.

Do not casually move a session-scoped object to a global singleton. State such as prompts, intent handling, hot credential caches, and transient-flow services must not leak between independent sessions.

## Android activity and session lifecycle

Android sessions are owned by `AndroidWalletSessionViewModel`, not directly by an Activity. This intentionally keeps the same session across configuration changes.

- `MainActivity` and `TransientFlowActivity` attach to the retained ViewModel/activity host.
- A configuration change detaches the old Activity and attaches its replacement without closing the session.
- A real Activity finish clears the ViewModel; `onCleared()` closes `SessionService` and clears Activity-hosted actions.
- DC API responses and Activity-only operations must go through the current Activity host. Do not capture an Activity in session-scoped services, long-lived lambdas, HTTP clients, or the platform adapter.
- `AndroidPlatformAdapter` must retain only the application context. Operations that truly require an Activity belong in the Activity host.
- If an Activity action occurs during the short recreation gap, it may be queued for the next Activity. Ensure final cleanup drops queued work.

When changing this lifecycle, test both paths separately:

1. recreate/rotate and verify the session survives;
2. finish the transient flow and verify the Activity and session close exactly once.

Android Activity objects are destroyed with `finish()`; sessions are closed through ViewModel cleanup. Process death does not guarantee managed close callbacks and must remain safe.

## iOS sessions and multi-process behavior

The iOS main app and Identity Document Provider extension are separate processes. They can access the same persistent data store concurrently, but they cannot share an in-memory Koin scope, coroutine scope, hot cache, or wallet session.

- Each process requires its own session and in-memory state.
- Never design synchronization around a process-global singleton alone.
- Persistent storage is the cross-process source of truth.
- Preserve the Darwin inter-process coordination used by the iOS DataStore implementation.
- A hot cache in one process may lag a write made by the other process; correctness must come from persistent writes and observable invalidation, not shared-memory assumptions.

Apple simulator/device compilation requires the appropriate Apple toolchain and Digital Credentials cinterop. On Linux, `compileKotlinIosSimulatorArm64` can fail because `DigitalCredentials` is unavailable even when common and platform-independent code is correct.

## Credential storage

Credential storage has two layers:

- `PersistentSubjectCredentialStore`: the cold, durable source of truth backed by `DataStoreService`.
- `HotWalletSubjectCredentialStore`: a session-local `StateFlow` snapshot used to avoid repeatedly decoding the entire credential container.

`WalletSubjectCredentialStore` contains common read behavior:

- `getCredentials()` reads from the implementation's `observeStoreContainer()` and applies scheme filtering.
- `getInvalidCredentials()` reads the same container and validates freshness.

The hot implementation must explicitly call the `WalletSubjectCredentialStore` default `getCredentials()` implementation. Kotlin interface delegation can otherwise silently select the persistent delegate and bypass the hot snapshot.

Mutation rules:

- `removeStoreEntryById()` and `reset()` must write to persistent storage immediately.
- Never make a durable mutation only in the hot store. Other sessions and the separate iOS process would continue seeing the old data, and the next cold-store emission could restore it.
- The hot store should update as a consequence of observing the persistent store.

Credential-container JSON/CBOR serialization is CPU-intensive. Keep encode/decode work off the UI dispatcher. Do not reintroduce full-container deserialization into Compose/UI coroutines. Coordinate expensive serialization per process so multiple sessions do not saturate the CPU with duplicate work.

## Coroutines and Compose

`LaunchedEffect` is tied to composition lifetime. Navigating away or finishing an Activity cancels its coroutine immediately on the UI thread.

- Use `LaunchedEffect` for UI-lifetime work only.
- Put session-lifetime business work in the session coroutine scope when it must survive recomposition or route replacement.
- Preserve structured cancellation; do not hide leaks by launching into an unrelated global scope.
- Cancellation callbacks must not perform blocking disk or network I/O on the cancelling thread.
- Moving a coroutine body to `Dispatchers.IO` does not necessarily move synchronous completion handlers; inspect cancellation behavior as well.

Avoid expensive parsing, serialization, cryptography, or blocking platform calls on the main thread. ANRs in this project have been caused by decoding a large persisted credential container on the Compose dispatcher.

## HTTP clients

Never rely on Ktor engine auto-discovery and do not create bare `HttpClient()` instances.

The platform engine is selected through `createPlatformHttpClientEngine()`:

- Android: CIO. Do not switch back to Ktor's `HttpURLConnection`/Android engine; cancelling an unfinished response from a main-thread `LaunchedEffect` can synchronously close/drain its stream and throw `NetworkOnMainThreadException`.
- iOS: Darwin.

Build normal clients through `HttpService`. Components such as `PresentationService` that require a raw engine must use the same platform engine provider.

For every request:

- consume the response body (`body`, `bodyAsText`, `bodyAsBytes`, etc.) or explicitly close/cancel it;
- close short-lived clients in `finally`;
- prefer reusing an appropriately owned client over constructing a client per request; and
- ensure an owning service closes long-lived clients when its lifecycle ends.

An HTTP status probe still needs to consume or close its response. Merely checking that `get()` returned successfully can leave a chunked/gzip stream open until cancellation.

## Digital Credentials API

Android DC API matching uses bundled WebAssembly assets:

- verification: `androidApp/src/main/assets/dcapimatcher.wasm`;
- issuance: `androidApp/src/main/assets/dcapimatcher_issuing_hardcoded.wasm`.

Sources and build instructions are under `shared/src/androidMain/kotlin/matcher*`. If matcher source changes, rebuild and replace the corresponding asset; otherwise the application continues executing the old binary.

On Android, `credentialIds` supplied by the DC API are fixed credential selections made by the system. They are store-entry IDs, not DCQL credential-query IDs. Matching filters the credential store by these IDs; corresponding requested credential types can then be identified from the non-empty `DCQLMatchingResult.credentialQueryMatches` entries.

Keep "no fixed selection" distinct from "fixed selection with no matches." UI highlighting or fading must only activate when the original request actually contains `credentialIds`; otherwise an empty selected-query set could incorrectly make every requested type appear unselected.

The iOS DC API path includes both the main app and Identity Document Provider extension. Consider process boundaries and entitlements when changing it.

## Build and verification

Use the smallest checks that cover the change, then broaden for lifecycle, DI, persistence, or source-set changes.

Common Android verification:

```bash
./gradlew :androidApp:compileDebugKotlin :shared:testAndroidHostTest
```

Common metadata verification:

```bash
./gradlew :shared:compileCommonMainKotlinMetadata
```

On a compatible macOS host, also compile the relevant iOS target and run iOS tests. A Linux iOS failure consisting only of unresolved `DigitalCredentials` cinterop symbols is an environment limitation; do not classify unrelated Kotlin errors as the same limitation.

Before handing off:

```bash
git diff --check
git status --short
```

Do not overwrite or discard unrelated working-tree changes. Generated local files, signing material, `local.properties`, provisioning profiles, certificates, and local Xcode signing configuration must not be committed.

User-visible changes require an entry in the current unreleased section of `CHANGELOG.md` on the same branch. When splitting a dirty worktree into several PR branches, base each branch directly on `development`, keep its changelog entry with the corresponding change, and verify that the branch builds without depending on changes assigned elsewhere.

Treat nested Git checkouts and generated Gradle or IDE files as local artifacts unless the repository explicitly tracks them. Inspect untracked directories before staging broadly, especially around submodules and composite builds.

## High-risk change checklist

For storage changes:

- verify hot reads use the session snapshot;
- verify mutations reach persistent storage before reporting success;
- consider concurrent sessions and separate iOS processes;
- keep serialization off main.

For Android lifecycle changes:

- verify rotation retains the session;
- verify transient-flow completion finishes the Activity;
- verify session cleanup happens exactly once;
- verify no Activity is retained after destruction.

For networking changes:

- verify Android still uses CIO and iOS still uses Darwin;
- verify response bodies and clients have clear ownership;
- test cancellation while a request is in flight.

For shared API changes:

- compile Android and common metadata;
- check both Android and iOS actual implementations;
- consider how the iOS extension process constructs and closes its own session.

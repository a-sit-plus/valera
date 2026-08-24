# JetBrains Dev Container pilot

Open the repository locally, open the required
`.devcontainer/<profile>/devcontainer.json`, and use the Dev Container gutter action.
JetBrains documents both actions in
[Start Dev Container inside IDE](https://www.jetbrains.com/help/idea/start-dev-container-inside-ide.html).

## Choose the source workflow

Use **Create Dev Container and Mount Sources** for normal day-to-day development:

- The current local checkout is bind-mounted into the container.
- Uncommitted changes are immediately available for builds and tests.
- Edits are synchronized in both directions; the container can modify or delete files in
  that checkout. No Docker socket, device, broad host-home directory, or sibling checkout
  is mounted.
- Local `local.properties` and wallet debug-signing files are used or created according to
  the setup rules below. Existing values are preserved.

Use **Create Dev Container and Clone Sources** for clean, reproducible verification:

- JetBrains clones the selected remote branch into a Docker source volume. Local
  uncommitted changes are not included.
- Use this mode for onboarding, isolated experiments, and final handoff checks that prove
  a pushed branch works from a clean source volume.
- JetBrains initially creates this checkout with its helper container, then normalizes its
  ownership to the configured remote user. The narrowly scoped `CHOWN` capability in
  these profiles is required for that bootstrap step with Clone Sources.
- The workspace setup command is copied into the image, so it also works while testing a
  local Dev Container configuration against a remote branch that does not contain the
  new `.devcontainer` files yet. Commit and push the configuration before other users or
  clean machines rely on it.

In both workflows, profile-provided VC-K and Signum checkouts remain in separate named
Docker volumes under `/home/dev/composites`; host-side sibling checkouts are intentionally
not mounted. Therefore, uncommitted changes in a local sibling VC-K or Signum repository
are not visible to a consumer container.

## GitHub SSH host trust

The image preloads GitHub's published Ed25519 host key in
`/etc/ssh/ssh_known_hosts`. Its fingerprint is
`SHA256:+DiY3wvvV6TuJJhbpZisF/zLDA0zPMSvHdkr4UvCOqU`, so SSH-based Git
operations do not require an interactive first-connection confirmation. This establishes
the identity of `github.com`; it does not provide user authentication or copy private
keys into the container. If GitHub rotates the key, update the pinned entry from
[GitHub's SSH key fingerprints](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/githubs-ssh-key-fingerprints).

## Gradle JVM in IntelliJ

The Dev Container exposes `JAVA_HOME` explicitly to the remote IDE process:
`/opt/java/temurin-17` for Wallet, VC-K, and Signum, and
`/opt/java/temurin-21` for the backend repositories. Workspace setup removes only
persisted Gradle-JVM overrides from the ignored `.idea/gradle.xml` and
`java.home` from the ignored `.gradle/config.properties`. Such absolute paths are
otherwise validated in both the local IntelliJ frontend and the container target, where
a host-only path or a container-only `/opt` path cannot be valid in both namespaces.

With no project override, IntelliJ resolves the Gradle JVM from the remote
`JAVA_HOME`, as described in
[Gradle JVM selection](https://www.jetbrains.com/help/idea/gradle-jvm-selection.html).
After upgrading an existing container, rebuild it so that the new `remoteEnv` is
applied, rerun `wallet-setup-workspace`, reopen the project if necessary, and reload
all Gradle projects. Do not manually persist an absolute Gradle JVM path in the project.

## Kotlin compiler process

Workspace setup no longer writes `kotlin.compiler.execution.strategy` anywhere. Kotlin
compilation uses its normal daemon strategy.

An earlier revision injected `kotlin.compiler.execution.strategy=in-process` into the
shared Gradle user `gradle.properties` to avoid `Storage ... is already registered`
during composite imports. That override had a much larger blast radius than intended:
`GRADLE_USER_HOME` is the `wallet-internal-gradle-v1` volume shared by every repository
and profile, so the setting silently applied everywhere and survived image rebuilds and
container deletion. It also runs the Kotlin compiler inside the Gradle daemon JVM,
sharing a class-loader space with Gradle's own embedded Kotlin and kotlin-reflect, which
can itself produce kotlin-reflect version-skew failures during sync.

The underlying cache collision comes from sharing one `GRADLE_USER_HOME` across
repositories. If `Storage ... is already registered` reappears, give each repository its
own Gradle user home instead of overriding the compiler execution strategy globally.

To clear the override from an existing shared volume:

```bash
docker run --rm -v wallet-internal-gradle-v1:/g busybox \
  sed -i '/^[[:space:]]*kotlin\.compiler\.execution\.strategy[[:space:]]*=/d' /g/gradle.properties
```

## IntelliJ Gradle module layout

Workspace setup sets `resolveModulePerSourceSet=false` in the ignored
`.idea/gradle.xml`. IntelliJ IDEA 2026.2.1 can otherwise crash after model resolution in
`mergeSourceSetContentRootsInModulePerSourceSetMode` when a composite Kotlin source
root has no matching content-root ancestor. Importing one IntelliJ module per Gradle
project bypasses that broken merge path. IntelliJ also treats this layout as incompatible
with phased Gradle sync and automatically uses its non-phased import path.

After changing this setting in an already open project, close and reopen the project
before reloading Gradle so the external-system settings are reconstructed from the XML.

## IDE backend plugins


`customizations.jetbrains.plugins` is intentionally empty. See
[JetBrains plugin installation for Dev Containers](https://youtrack.jetbrains.com/articles/SUPPORT-A-737/How-to-set-up-automatical-plugin-installation-inside-the-dev-container)
for the mechanism.

## Kotlin Multiplatform plugin breaks Gradle sync on a target

The Kotlin Multiplatform plugin (`com.jetbrains.kmm`) must be **disabled in the IDE that
runs the sync**. Its `intellij.kmm.compose.desktop.jar` module contains
`ComposeHotReloadGradleProjectResolver`, which requests the
`org.jetbrains.compose.reload.gradle.idea.IdeaComposeHotReloadModel` tooling model
unconditionally — there is no setting that gates it, and it fires even for projects that
never apply the Compose Hot Reload Gradle plugin, as is the case in all these
repositories.

When Gradle runs on a target, IntelliJ uploads only a subset of tooling-extension jars
into the container (`intellij.kmm.gradle.tooling.rt.jar`,
`intellij.compose.ide.plugin.gradleTooling.rt.jar`, and similar).
`intellij.kmm.compose.desktop.jar` is *not* among them, so
`org.jetbrains.plugins.gradle.tooling.proxy.Main` cannot resolve the requested model class
while deserializing the build parameters. The connection dies before Gradle starts:

```
java.lang.ClassNotFoundException: org.jetbrains.compose.reload.gradle.idea.IdeaComposeHotReloadModel
    at ...ClassLoaderObjectInputStream.resolveClass
Exception in thread "main" java.lang.IllegalStateException: Target build parameters were not received
```

Disable it where the IDE actually runs. In the current setup the IDE runs on the **host**
and executes Gradle in the container as a target; there is no IDE backend inside the
container (`/.jbdevcontainer/JetBrains/RemoteDev/dist` is empty and no backend process is
present). Setting `customizations.jetbrains.plugins` therefore has no effect on this
failure — that list only *installs* plugins into a backend, it cannot disable one, and
here there is no backend to install into. Use **Settings | Plugins | Kotlin
Multiplatform | Disable**, then restart the IDE. The equivalent config entry is
`com.jetbrains.kmm` in `~/.config/JetBrains/IntelliJIdea<version>/disabled_plugins.txt`,
which must only be edited while the IDE is closed.

Base Kotlin Multiplatform Gradle import comes from the bundled Kotlin plugin and is
unaffected. These containers also set `disableAppleTargets=true`, so the plugin's
Apple-target features are inert here regardless.

`customizations.jetbrains.plugins` is left empty so that the plugin is not reintroduced if
these profiles are ever used with a real in-container backend. If a future IntelliJ build
ships `intellij.kmm.compose.desktop.jar` in the tooling upload set, the plugin can be
enabled again. Verify with a target sync, not a host-only sync — a local Gradle run
injects the resolver's classes directly and never reproduces this.

IntelliJ IDEA 2026.2 can launch Gradle through an isolated target proxy whose class path
does not include the Compose Hot Reload model requested by the Kotlin Multiplatform
plugin. The image still contains the pinned
`org.jetbrains.compose.hot-reload:hot-reload-gradle-idea:1.0.0` model used by Compose
Multiplatform 1.10.3 at `/opt/jetbrains-tooling/hot-reload-gradle-idea.jar`, but it is no
longer exposed through `JAVA_TOOL_OPTIONS`. When upgrading Compose beyond the 1.10 line,
verify the Hot Reload version preferred by the published Compose Gradle plugin metadata
and update both the artifact URL and SHA-256.

Do not put this jar on the bootstrap class path. An earlier revision set
`JAVA_TOOL_OPTIONS=-Xbootclasspath/a:/opt/jetbrains-tooling/hot-reload-gradle-idea.jar`,
which broke Gradle sync. The jar contains only
`org.jetbrains.compose.reload.gradle.idea.*` and depends on kotlin-stdlib and
kotlinx-serialization, which it does not ship. Appending it to the bootstrap class path
means:

- The classes are defined by the bootstrap loader, which cannot see kotlin-stdlib or
  kotlinx-serialization, so they fail to link
  (`NoClassDefFoundError: kotlinx/serialization/internal/SerializationConstructorMarker`).
- Parent-first delegation makes that broken copy permanently shadow the correct copy the
  Compose Gradle plugin puts on the class path, so the intended fix can never take effect.
- The JVM silently drops the `@kotlin.Metadata` annotation because the annotation type is
  not visible to the defining loader. kotlin-reflect then sees a class with no Kotlin
  metadata and reports `Property 'annotations' (JVM signature: getAnnotations()...) not
  resolved in class kotlin.reflect.jvm.internal.impl...`.
- `JAVA_TOOL_OPTIONS` applies to every JVM in the container: the IDE backend, the Gradle
  launcher and daemon, Kotlin compilation, test JVMs, and `keytool`.

Verify the current image does not reintroduce it:

```bash
docker run --rm <image> java -XshowSettings:properties -version 2>&1 | grep -i bootclasspath
docker run --rm <image> printenv JAVA_TOOL_OPTIONS
```

If the Gradle target proxy genuinely lacks the model, add it to the *Gradle* class path
with an init script (`initscript { dependencies { classpath(files("/opt/jetbrains-tooling/hot-reload-gradle-idea.jar")) } }`),
where kotlin-stdlib is visible. Never through the bootstrap class path.

Profiles:

- `standalone` uses published dependencies only.
- `vck` provisions VC-K in a profile-specific volume.
- `vck-signum` provisions VC-K and Signum in that volume.
- VC-K itself offers `standalone` and `signum`; Signum offers only `standalone`.

Existing composite checkouts are never pulled, switched, reset, or overwritten. The setup
tries the consumer branch, `develop`, `development`, and finally the remote default
branch when it must clone a missing checkout. Main-checkout and VC-K submodules are
initialized recursively only after a dirty-submodule guard.

Before IntelliJ imports a VC-K composite, setup runs VC-K's aggregate
`generateProjectStructureMetadata` task. This pre-creates the Kotlin Multiplatform
project-structure JSON files that the IDE dependency resolver may otherwise try to read
before Gradle has produced them. The task is incremental on subsequent starts. If an
older container encounters a missing `kotlin-project-structure-metadata.json`, run
`wallet-setup-workspace` and reload the Gradle project.

### Composite branches and forks

The fallback branch must still be build-compatible with the consumer. For example, an
AGP 9 consumer running Gradle 9.6 cannot use an older VC-K `develop` checkout that
still uses AGP 8.x: Gradle 9.6 removed an internal Problems API used by AGP 8.x.

For an unmerged matching branch, set these variables in the host environment that
launches IntelliJ:

```bash
export VCK_COMPOSITE_URL=https://github.com/<owner>/vck.git
export VCK_COMPOSITE_BRANCH=<matching-branch>

# Only needed by profiles that provision Signum:
export SIGNUM_COMPOSITE_URL=https://github.com/<owner>/signum.git
export SIGNUM_COMPOSITE_BRANCH=<matching-branch>
```

Do not put these values only in `~/.bashrc` or assume that `~/.profile` reaches every
desktop session: a GUI-launched IntelliJ does not inherit variables from an interactive
Bash startup file, and desktop startup behavior varies. KDE Plasma users should create
`~/.config/plasma-workspace/env/wallet-devcontainers.sh` containing the literal exports:

```bash
export VCK_COMPOSITE_URL=https://github.com/<owner>/vck.git
# Optional: omit this to use the consumer-branch fallback order.
export VCK_COMPOSITE_BRANCH=<matching-branch>
```

Plasma executes exported variables from `~/.config/plasma-workspace/env/*.sh` when the
session starts. Close every IntelliJ process, sign out of Plasma completely, and sign
back in before creating the container. `~/.profile` may additionally source the same
file for login shells. On any desktop, starting IntelliJ from the same terminal in which
the variables were exported is a reliable immediate alternative. Commands such as
`gh api` should remain an explicit host-side step instead of running during every login.

For a conventionally named personal fork, GitHub CLI can supply the authenticated
account name without hard-coding it:

```bash
gh auth status
github_login="$(gh api user --jq .login)"
export VCK_COMPOSITE_URL="https://github.com/$github_login/vck.git"
export VCK_COMPOSITE_BRANCH=<matching-branch>
unset github_login

# Start IntelliJ from this environment; for the Snap installation:
snap run intellij-idea-ultimate
```

When the local VC-K checkout's `origin` may use a different owner or repository name,
run this inside that checkout instead:

```bash
export VCK_COMPOSITE_URL="$(gh repo view --json url --jq '.url + ".git"')"
```

Both `gh` commands run on the host. GitHub CLI credentials are not copied or mounted
into the Dev Container.

The relevant profiles pass the values using `${localEnv:...}`, which JetBrains lists
among its [supported Dev Container variables](https://www.jetbrains.com/help/idea/prerequisites-for-dev-containers.html).
Unset URL variables retain the canonical A-SIT Plus HTTPS remotes; unset branch
variables retain the normal fallback order. An explicit branch must exist at the
selected URL or setup fails instead of silently choosing another branch.

Before creating the container, verify the environment seen by the IntelliJ launcher:

```bash
printf 'VCK_COMPOSITE_URL=%s\nVCK_COMPOSITE_BRANCH=%s\n' \
  "$VCK_COMPOSITE_URL" "$VCK_COMPOSITE_BRANCH"
```

After creation, verify that JetBrains forwarded both values and inspect the actual
persistent checkout:

```bash
docker inspect <container-id> --format '{{range .Config.Env}}{{println .}}{{end}}' \
  | grep '^VCK_COMPOSITE_'
docker exec <container-id> git -C /home/dev/composites/vck remote get-url origin
docker exec <container-id> git -C /home/dev/composites/vck branch --show-current
```

Overrides only affect a new, empty composite volume. To replace an incompatible
persisted checkout, first confirm that it contains no work that must be kept:

```bash
docker exec <container-id> git -C /home/dev/composites/vck status --short
docker exec <container-id> git -C /home/dev/composites/vck branch --show-current
```

Commit or copy out any required changes, remove the Dev Container in JetBrains, and
then remove only that profile's composite volume:

```bash
docker volume rm <profile-composite-volume>
```

For example, the Compose Wallet `vck` profile uses
`compose-wallet-app-vck-profile-v1`. Removing this volume permanently deletes its
VC-K checkout, but does not delete the source or shared Gradle/Kotlin/Node cache
volumes. Recreate the Dev Container from the same host environment so setup clones the
requested URL and branch.

## Rootless Docker prerequisite

The pilot is accepted only with a rootless Docker daemon. Follow [Docker's official rootless installation instructions](https://docs.docker.com/engine/security/rootless/), then select and verify the rootless context:

```bash
docker context use rootless
docker context inspect rootless
docker info --format '{{json .SecurityOptions}}'
docker info --format '{{.DockerRootDir}}'
```

The active endpoint must be the per-user socket (normally
`unix://$XDG_RUNTIME_DIR/docker.sock`), and the security options must contain
`name=rootless`, `name=seccomp,profile=builtin`, and a user namespace. A root-owned
`/var/run/docker.sock` does not pass acceptance.

After JetBrains starts a container, inspect it from the host:

```bash
docker inspect <container-id> --format   'User={{.Config.User}} CapDrop={{json .HostConfig.CapDrop}} CapAdd={{json .HostConfig.CapAdd}} SecurityOpt={{json .HostConfig.SecurityOpt}} Pids={{.HostConfig.PidsLimit}} CPUs={{.HostConfig.NanoCpus}} Memory={{.HostConfig.Memory}}'
docker inspect <container-id> --format '{{json .Mounts}}'
docker inspect <container-id> --format '{{json .HostConfig.Devices}}'
```

Expected: user `dev`, `ALL` capabilities dropped and only `CHOWN` re-added,
`no-new-privileges`, PID limit 4096, 12 CPUs, 24 GiB RAM, no Docker socket or
device, and no broad host-home mount. `CHOWN` lets JetBrains change the helper-cloned
source volume from root ownership to the adapted remote UID; it is not an effective
capability of the non-root `dev` process. Verify that separately with:

```bash
docker exec <container-id> grep -E "^(CapEff|CapBnd):" /proc/1/status
```

`CapEff` must be all zeroes. A bind mount for the selected repository is expected when
using Mount Sources. The built-in Docker seccomp profile remains active because no
override is supplied.

`/tmp` is executable because JetBrains stages its remote-agent binary there; it remains
limited to 2 GiB and mounted with `nosuid,nodev`. `/run` remains a limited non-executable
tmpfs.

## JetBrains UID-image cache troubleshooting

JetBrains may reuse its generated `jb-...-uid` image after rebuilding the configured base
image. If `/usr/local/bin/wallet-setup-workspace` exists in the new base image but is
missing from the started container, remove the failed Dev Container in JetBrains, identify
the matching generated UID image, and remove only that image:

```bash
docker image ls --filter "reference=jb-*-uid"
docker image rm <matching-jb-uid-image>
```

Recreate the Dev Container so JetBrains regenerates the UID layer from the current base
image. Do not delete the source, composite, Gradle, Kotlin/Native, or npm volumes.

## Wallet debug signing

For compose-wallet-app only, setup creates a random source-volume-local debug PKCS#12
key when both `androidApp/keystore.p12` and `android.cert.password` are absent.
Existing or incomplete signing configuration is preserved.

To import a key, first copy it once from the host:

```bash
docker cp /host/path/keystore.p12 <container-id>:/tmp/wallet-import.p12
```

Then run `wallet-import-debug-keystore` in the container. It reads the password
without echo, validates password and alias, and requires the literal confirmation
`REPLACE` before replacing an existing key. Delete the temporary copied file
afterward.

## Pilot scope and acceptance

Phase 1 intentionally has unrestricted outbound networking and a writable root
filesystem. This is a documented security deviation, not full hardening. It does not
provide host ADB/USB, an emulator, Docker/Testcontainers orchestration, Codex, or Claude.

Phase 2 must add an allow-list proxy and internal network path, normal/update/offline
modes, a read-only root filesystem, an ADB broker, and separately reviewed agent access.

Suggested build checks:

- Signum: `./gradlew :indispensable-josef:jvmTest`
- VC-K: `./gradlew :vck:jvmTest`
- Compose Wallet: `./gradlew :androidApp:compileDebugKotlin :shared:testAndroidHostTest :shared:compileCommonMainKotlinMetadata`
- Issuing Backend: `./gradlew test :http:bootJar`
- Relying Party: `./gradlew :service:test` and
  `./gradlew -p android :app:testDebugUnitTest :app:assembleDebug`

The Android SDK is exposed to IntelliJ and Gradle as `~/.androidsdk`
(`/home/dev/.androidsdk`). It is a symlink to the image-baked `/opt/android-sdk`. The
profile also passes the host username as a validated image build argument and creates
the equivalent `/home/<host-user>/.androidsdk` symlink. This makes a host-style path
written or validated by IntelliJ valid inside the container as well. These are
image-local aliases; the SDK is not duplicated, and the host home directory is not
mounted. The host username is also passed to workspace setup, which uses this dual-valid
path in generated `local.properties` files and upgrades the previous managed
`/home/dev/.androidsdk` value.

The named `wallet-internal-*-v1` volumes are restricted to these trusted internal
repositories. Project `.gradle` directories remain in each source volume. Prove cache
reuse by warming dependencies in two different repository containers and running an
appropriate `--offline` build afterward.

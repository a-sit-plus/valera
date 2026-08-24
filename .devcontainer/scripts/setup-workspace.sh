#!/usr/bin/env bash
set -euo pipefail

profile="${WALLET_PROFILE:-standalone}"
repo_kind="${WALLET_REPOSITORY_KIND:-unknown}"
workspace="$(git rev-parse --show-toplevel)"
composite_root=/home/dev/composites
vck_composite_url="${VCK_COMPOSITE_URL:-https://github.com/a-sit-plus/vck.git}"
signum_composite_url="${SIGNUM_COMPOSITE_URL:-https://github.com/a-sit-plus/signum.git}"
host_user="${HOST_USER:-}"
android_sdk_dir=/home/dev/.androidsdk

# sdk.dir must resolve in BOTH namespaces. local.properties is consumed by container-side
# Gradle, but the host-side IntelliJ also validates it and silently rewrites the file to its
# own Android SDK when the path does not exist on the host ("The SDK path ... does not belong
# to a directory. IntelliJ IDEA will use this Android SDK instead ..."). A container-only path
# such as /opt/android-sdk therefore does not survive. The image creates
# /home/$HOST_USER/.androidsdk as a symlink to /opt/android-sdk so that the host-shaped path
# the IDE expects also resolves inside the container. Do not "simplify" this to a
# container-only path.
if [[ -n "$host_user" ]]; then
    [[ "$host_user" =~ ^[A-Za-z0-9._-]+$ ]] || {
        printf '[devcontainer] ERROR: Invalid HOST_USER: %s\n' "$host_user" >&2
        exit 1
    }
    if [[ -d "/home/$host_user/.androidsdk" ]]; then
        android_sdk_dir="/home/$host_user/.androidsdk"
    fi
fi

log() { printf '[devcontainer] %s\n' "$*"; }
fail() { printf '[devcontainer] ERROR: %s\n' "$*" >&2; exit 1; }

append_property_if_missing() {
    local file="$1" key="$2" value="$3"
    mkdir -p "$(dirname "$file")"
    touch "$file"
    if ! grep -Eq "^[[:space:]]*${key//./\\.}[[:space:]]*=" "$file"; then
        printf '%s=%s\n' "$key" "$value" >>"$file"
    fi
}

prepare_properties_file() {
    local properties="$1"
    if [[ -f "$properties" ]]; then
        # Normalize any previously written value, not just one known literal. A stale
        # host-shaped or host-only path must not survive into a container build.
        sed -i -E "s|^([[:space:]]*sdk\\.dir[[:space:]]*=).*$|\\1$android_sdk_dir|" "$properties"
    fi
    append_property_if_missing "$properties" sdk.dir "$android_sdk_dir"
    append_property_if_missing "$properties" disableAppleTargets true
    append_property_if_missing "$properties" disableNdkTargets true
}

prepare_properties() {
    prepare_properties_file "$workspace/local.properties"

    if [[ -x "$workspace/android/gradlew" ]]; then
        local android_properties="$workspace/android/local.properties"
        if [[ -f "$android_properties" ]]; then
            sed -i -E "s|^([[:space:]]*sdk\\.dir[[:space:]]*=).*$|\\1$android_sdk_dir|" "$android_properties"
        fi
        append_property_if_missing "$android_properties" sdk.dir "$android_sdk_dir"
    fi
}

prepare_intellij_gradle_jvm() {
    local gradle_xml="$workspace/.idea/gradle.xml"
    local local_gradle_config="$workspace/.gradle/config.properties"

    [[ -x "$JAVA_HOME/bin/java" ]] || fail "JAVA_HOME does not contain an executable Java runtime: $JAVA_HOME"

    mkdir -p "$(dirname "$gradle_xml")"
    if [[ ! -f "$gradle_xml" ]]; then
        printf '%s\n' \
            '<?xml version="1.0" encoding="UTF-8"?>' \
            '<project version="4">' \
            '  <component name="GradleSettings">' \
            '    <option name="linkedExternalProjectsSettings">' \
            '      <GradleProjectSettings>' \
            '        <option name="resolveModulePerSourceSet" value="false" />' \
            '        <option name="externalProjectPath" value="$PROJECT_DIR$" />' \
            '      </GradleProjectSettings>' \
            '    </option>' \
            '  </component>' \
            '</project>' >"$gradle_xml"
    elif grep -q '<option name="resolveModulePerSourceSet"' "$gradle_xml"; then
        sed -i -E 's|(<option name="resolveModulePerSourceSet" value=")[^"]*(" */>)|\1false\2|' "$gradle_xml"
    elif grep -q '<GradleProjectSettings>' "$gradle_xml"; then
        sed -i '/<GradleProjectSettings>/a\        <option name="resolveModulePerSourceSet" value="false" />' "$gradle_xml"
    else
        fail "Cannot configure the IntelliJ Gradle import mode in unexpected file structure: $gradle_xml"
    fi
    log "Configured IntelliJ to import one module per Gradle project, avoiding the broken source-set content-root merge path."

    # In native Dev Container mode this metadata is parsed by both the local
    # frontend and the container-side Gradle target. No absolute JDK path is
    # valid in both namespaces in general, so let IntelliJ resolve JAVA_HOME
    # from the explicitly configured remote environment instead.
    if [[ -f "$gradle_xml" ]] && grep -q '<option name="gradleJvm"' "$gradle_xml"; then
        sed -i -E '/<option name="gradleJvm" value="[^"]*" *\/>/d' "$gradle_xml"
        log "Removed the persisted IntelliJ Gradle JVM override from $gradle_xml."
    fi

    # #GRADLE_LOCAL_JAVA_HOME is backed by this ignored file. Android Studio
    # may have written a host-only runtime here before the project was opened
    # in a container.
    if [[ -f "$local_gradle_config" ]] && grep -Eq '^[[:space:]]*java\.home[[:space:]]*=' "$local_gradle_config"; then
        sed -i -E '/^[[:space:]]*java\.home[[:space:]]*=/d' "$local_gradle_config"
        log "Removed the host-specific Gradle java.home override from $local_gradle_config."
    fi

    log "IntelliJ Gradle JVM will be resolved from remote JAVA_HOME=$JAVA_HOME."
}

current_branch() {
    git -C "$workspace" symbolic-ref --quiet --short HEAD 2>/dev/null || true
}

clone_if_missing() {
    local name="$1" url="$2" destination="$3" requested_branch="${4:-}"
    if [[ -d "$destination/.git" ]]; then
        local existing_branch existing_commit existing_url
        existing_branch="$(git -C "$destination" symbolic-ref --quiet --short HEAD 2>/dev/null || printf detached)"
        existing_commit="$(git -C "$destination" rev-parse --short HEAD)"
        existing_url="$(git -C "$destination" remote get-url origin 2>/dev/null || printf unknown)"
        log "Using existing $name checkout at $destination ($existing_branch at $existing_commit, origin $existing_url); no pull or checkout performed."
        if [[ "$existing_url" != "$url" ]]; then
            log "Requested $name URL $url is not applied to an existing checkout. Use a new/empty composite volume to clone it."
        fi
        if [[ -n "$requested_branch" && "$existing_branch" != "$requested_branch" ]]; then
            log "Requested $name branch $requested_branch is not applied to an existing checkout. Use a new/empty composite volume to clone it."
        fi
        return
    fi
    if [[ -e "$destination" ]] && [[ -n "$(find "$destination" -mindepth 1 -maxdepth 1 -print -quit 2>/dev/null)" ]]; then
        fail "$destination exists but is not a Git checkout; refusing to overwrite it."
    fi

    if [[ -n "$requested_branch" ]]; then
        if ! git ls-remote --exit-code --heads "$url" "refs/heads/$requested_branch" >/dev/null 2>&1; then
            fail "Requested $name branch $requested_branch does not exist at $url."
        fi
        log "Cloning $name branch $requested_branch from $url (explicit override)."
        git clone --branch "$requested_branch" --single-branch "$url" "$destination"
        return
    fi

    local candidate branch
    branch="$(current_branch)"
    for candidate in "$branch" develop development; do
        [[ -n "$candidate" ]] || continue
        if git ls-remote --exit-code --heads "$url" "refs/heads/$candidate" >/dev/null 2>&1; then
            log "Cloning $name branch $candidate from $url."
            git clone --branch "$candidate" --single-branch "$url" "$destination"
            return
        fi
    done

    log "Cloning $name using its remote default branch."
    git clone "$url" "$destination"
}

update_submodules_safely() {
    local checkout="$1" label="$2" dirty
    [[ -f "$checkout/.gitmodules" ]] || return 0
    dirty="$(
        git -C "$checkout" submodule foreach --quiet --recursive             'if [ -n "$(git status --porcelain)" ]; then printf "%s\n" "$displaypath"; fi' 2>/dev/null || true
    )"
    if [[ -n "$dirty" ]]; then
        printf '[devcontainer] Dirty submodule(s) in %s:\n%s\n' "$label" "$dirty" >&2
        fail "Submodule setup stopped to protect user work. Commit, stash, or clean those submodules explicitly."
    fi
    log "Initializing recursive submodules for $label."
    git -C "$checkout" submodule update --init --recursive
}

prepare_vck_ide_metadata() {
    local checkout="${VCK_COMPOSITE_PATH:-}"
    [[ -n "$checkout" && -d "$checkout/.git" ]] || return 0
    [[ -x "$checkout/gradlew" ]] || fail "VC-K Gradle wrapper is missing or not executable at $checkout/gradlew."

    log "Generating VC-K Kotlin project-structure metadata for IntelliJ composite import."
    (
        cd "$checkout"
        ./gradlew generateProjectStructureMetadata --no-daemon --console=plain
    )
}

prepare_wallet_debug_key() {
    [[ "$repo_kind" == compose-wallet-app ]] || return 0
    local key="$workspace/androidApp/keystore.p12"
    local properties="$workspace/local.properties"
    local has_password=false
    grep -Eq '^[[:space:]]*android\.cert\.password[[:space:]]*=' "$properties" && has_password=true

    if [[ -f "$key" || "$has_password" == true ]]; then
        if [[ ! -f "$key" || "$has_password" != true ]]; then
            log "Wallet signing setup is incomplete; preserving it unchanged. Add the missing key or password manually."
        fi
        return
    fi

    local password
    password="$(openssl rand -hex 24)"
    log "Generating a source-volume-local self-signed Android debug key."
    keytool -genkeypair -noprompt -storetype PKCS12 -keystore "$key"         -storepass "$password" -keypass "$password" -alias key0         -keyalg RSA -keysize 3072 -validity 3650         -dname 'CN=Wallet Dev Container Debug,O=A-SIT Plus Development,C=AT'
    chmod 0600 "$key"
    printf 'android.cert.password=%s\n' "$password" >>"$properties"
    unset password
}

mkdir -p "$composite_root"
prepare_properties
prepare_intellij_gradle_jvm

case "$profile" in
    standalone)
        log "Standalone profile: no composite repositories are provisioned."
        ;;
    vck)
        [[ -n "${VCK_COMPOSITE_PATH:-}" ]] || fail "VCK_COMPOSITE_PATH is required for profile vck."
        clone_if_missing VC-K "$vck_composite_url" "$VCK_COMPOSITE_PATH" "${VCK_COMPOSITE_BRANCH:-}"
        ;;
    signum)
        [[ -n "${SIGNUM_COMPOSITE_PATH:-}" ]] || fail "SIGNUM_COMPOSITE_PATH is required for profile signum."
        clone_if_missing Signum "$signum_composite_url" "$SIGNUM_COMPOSITE_PATH" "${SIGNUM_COMPOSITE_BRANCH:-}"
        ;;
    vck-signum)
        [[ -n "${VCK_COMPOSITE_PATH:-}" && -n "${SIGNUM_COMPOSITE_PATH:-}" ]]             || fail "Both composite paths are required for profile vck-signum."
        clone_if_missing VC-K "$vck_composite_url" "$VCK_COMPOSITE_PATH" "${VCK_COMPOSITE_BRANCH:-}"
        clone_if_missing Signum "$signum_composite_url" "$SIGNUM_COMPOSITE_PATH" "${SIGNUM_COMPOSITE_BRANCH:-}"
        ;;
    *)
        fail "Unknown profile: $profile"
        ;;
esac

if [[ -n "${VCK_COMPOSITE_PATH:-}" && -d "$VCK_COMPOSITE_PATH/.git" ]]; then
    prepare_properties_file "$VCK_COMPOSITE_PATH/local.properties"
fi
if [[ -n "${SIGNUM_COMPOSITE_PATH:-}" && -d "$SIGNUM_COMPOSITE_PATH/.git" ]]; then
    prepare_properties_file "$SIGNUM_COMPOSITE_PATH/local.properties"
fi

update_submodules_safely "$workspace" "main checkout"
if [[ -n "${VCK_COMPOSITE_PATH:-}" && -d "$VCK_COMPOSITE_PATH/.git" ]]; then
    update_submodules_safely "$VCK_COMPOSITE_PATH" "VC-K composite"
fi
prepare_vck_ide_metadata
prepare_wallet_debug_key
log "Workspace setup complete for profile $profile."

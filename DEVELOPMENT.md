# Compose Multiplatform Wallet App

## Development

Development happens in branch `development`. The `main` branch always tracks the latest release. Hence, create PRs against `development`. Use dedicated `release/x.y.z` branches to prepare releases and create release PRs against `main`, which will then be merged back into `development`.

### Using published VC-K dependencies

When a sibling `../vck` checkout exists, the build automatically includes it as a Gradle composite build. This is useful when developing Valera and VC-K together.

Set `disableVckComposite=true` when you instead want to use the published VC-K dependencies while keeping the sibling checkout in place. The option can be supplied as an environment variable:

```bash
disableVckComposite=true ./gradlew :androidApp:assembleDebug
```

It can also be passed as a Gradle project property:

```bash
./gradlew -PdisableVckComposite=true :androidApp:assembleDebug
```

For a persistent local setting, add the following to the root `local.properties` or `gradle.properties` file:

```properties
disableVckComposite=true
```

### Disabling Apple targets

Set `disableAppleTargets=true` when only Android/JVM targets are needed, such as for Android-only development on a non-macOS host. This prevents the shared module from declaring Apple targets and omits the `cinterop` and `interop` modules that bridge the Apple Digital Credentials API.

The option can be supplied as an environment variable:

```bash
disableAppleTargets=true ./gradlew :androidApp:assembleDebug
```

It can also be passed as a Gradle project property:

```bash
./gradlew -PdisableAppleTargets=true :androidApp:assembleDebug
```

For a persistent local setting, add it to the root `local.properties` or `gradle.properties` file:

```properties
disableAppleTargets=true
```

### Android test runners without sudo

`.github/workflows/test-android.yml` uses a self-hosted Linux runner for private repositories. It skips the shared setup action's privileged KVM configuration and checks that the runner can read and write `/dev/kvm` before running the emulator tests. Public GitHub-hosted runners continue to use the shared KVM setup.

Provision KVM on the self-hosted machine once, outside the workflow:

* Enable hardware virtualization and ensure the host exposes `/dev/kvm`. A VM host also needs nested virtualization enabled by its administrator.
* Give the runner account read/write access to `/dev/kvm`, typically through membership in the device's `kvm` group. Restart the runner service after changing its group membership.
* For Docker runners, pass the device into the container (`--device=/dev/kvm`) and give the container's runner user access to the device's numeric group ID, for example with `--group-add` set to the host device's group ID. Membership in a container group named `kvm` alone is insufficient if its numeric ID differs from the device's.
* Preinstall the Android SDK/emulator prerequisites and make SDK/cache directories writable by the runner account. The workflow does not install OS packages or grant device permissions on the private runner.

The administrator may need elevated access for host provisioning; the workflow runner itself does not need sudo. Emulator tests remain enabled and fail with a provisioning message if KVM access is missing.

## Local deployments

Building of the Android App locally requires a signer certificate to be configured. To do this you will need to generate a keystore file and add the keystore's password to your `local.properties` file.

> [!NOTE]
> The following instructions are for fine for locally building debug APKs. Inside a CI pipeline, it is highly recommended to use CI secrets that are propagated to environment veriables!

Navigate in a temporary directory and run the following commands to generate a keystore file:

```bash
# Create the Self-Signed Certificate
openssl req -x509 -newkey rsa:4096 -keyout private.key -out certificate.crt -days 365 -nodes
# Convert to .p12 Format. You will be asked for a password here, remember it!
openssl pkcs12 -export -out keystore.p12 -inkey private.key -in certificate.crt -name "key0"
# Move to the android app directory
mv keystore.p12 <path-to-repo>/androidApp/keystore.p12
```

Add the password property (`android.cert.password`) to the `local.properties` file in the root of the project:

```txt
sdk.dir=<path-to-android-sdk>
android.cert.password=<your-keystore-password>
```

## iOS signing

The repository is configured so public contributors can build the iOS app for the Simulator without access to the A-SIT Apple Developer team.

Public contributors:
* Open `iosApp/iosApp.xcodeproj` in Xcode and run the `iosApp` scheme on an iOS Simulator.
* Simulator builds for Apple Silicon should work without any additional signing setup.
* Running on the Simulator does not require an Apple Developer team, provisioning profile, or bundle identifier changes.
* Public contributors do not need `Signing.local.xcconfig`.
* Optional device builds with a Personal Team or another external team can use a local `Signing.local.xcconfig` with a unique bundle identifier and team id.
* Public Debug builds default to reduced entitlements, so `Associated Domains` and NFC are not available unless you are using internal signing. The reduced Debug entitlements still include the Identity Document Services document types needed for Digital Credentials registration, but the active provisioning profile must also allow that entitlement.
* Public contributors cannot run the app on a physical iPhone with the official bundle identifier `at.asitplus.wallet.compose`. Apple requires that bundle identifier to be signed by the team that owns it.
* Device builds and release archives for the official app identifier therefore require access to the A-SIT Apple Developer team.

Internal team members:
* Device builds use Xcode automatic signing in the `Debug` configuration.
* Copy [iosApp/Configuration/Signing.example.xcconfig](iosApp/Configuration/Signing.example.xcconfig) to `iosApp/Configuration/Signing.local.xcconfig`.
* Set `SIGNING_DEVELOPMENT_TEAM` in `Signing.local.xcconfig`.
* Set `SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp.entitlements` if you want `Associated Domains` and NFC in local Debug device builds.
* Set `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER` and `SIGNING_RELEASE_EXTENSION_PROVISIONING_PROFILE_SPECIFIER` in `Signing.local.xcconfig` if you want to create release archives locally.

Minimal `Signing.local.xcconfig` for internal users:

```xcconfig
APP_BUNDLE_IDENTIFIER = at.asitplus.wallet.compose
SIGNING_DEVELOPMENT_TEAM = 9CYHJNG644
SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp.entitlements
SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Distribution
SIGNING_RELEASE_EXTENSION_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Extension Distribution
```

`SIGNING_DEBUG_ENTITLEMENTS_FILE` is resolved relative to the Xcode project directory (`iosApp/`), so use `iosApp/iosApp.entitlements` here. Using `iosApp/iosApp/iosApp.entitlements` makes Xcode look for a doubled path and fail to open the file.

Minimal `Signing.local.xcconfig` for external device builds:

```xcconfig
APP_BUNDLE_IDENTIFIER = com.example.valera.dev
SIGNING_DEVELOPMENT_TEAM = YOUR_TEAM_ID
```

External device build limitations:
* Use a bundle identifier that is unique within your own Apple Developer team, for example `com.example.valera.dev`.
* Do not use `at.asitplus.wallet.compose` unless you are signing with the A-SIT Apple Developer team.
* `Associated Domains` and NFC are disabled in public Debug builds, so features that depend on those capabilities are not available on external device builds.

For release builds:
* The `Release` configuration uses manual signing for both the app and the `IdentityDocumentProviderExtension` target.
* Maintainers can provide `SIGNING_DEVELOPMENT_TEAM`, `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER`, and `SIGNING_RELEASE_EXTENSION_PROVISIONING_PROFILE_SPECIFIER` in the untracked `iosApp/Configuration/Signing.local.xcconfig`.
* CI should inject the signing certificate and provisioning profile from secrets at runtime instead of committing signing material to the repository.

Do not commit any of the following:
* `iosApp/Configuration/Signing.local.xcconfig`
* `*.mobileprovision`
* `*.p12`
* `*.cer`

Recommended GitHub Actions secret names for iOS release signing:
* `APPLE_CERT_CONTENT`
* `APPLE_CERT_PASSWORD`
* `APPLE_API_KEY_CONTENT`
* `APPLE_API_KEY_ID`
* `APPLE_API_ISSUER_ID`
* `APPLE_ID`

## Deployments

We use [fastlane](https://fastlane.tools/) to build the iOS App. The CI pipeline and secrets on this GitHub repository are already set up correctly.

Setup:
 - Get an Apple Development Account
 - Create an [App Store Connect API Key](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api) (with `App manager` access) and download it
 
Create a new certificate:
 - On your Mac, go to Keychain Access
 - Under Certificates, select the Apple Worldwide Developer Relations Certification Authority
 - In the menu go to Certificate Assistant, Request a Certificate from a Certificate Authority
 - There, enter your mail address, and set "Valera" as the Common Name, and save the CSR to disk
 - On the [Apple developer website](https://developer.apple.com/account/resources/certificates/add), create a certificate for `Apple development` and one for `Apple distribution` with the CSR generated before
 - Import the generated certificates into Keychain Access, to associate them with your key
 - Select both entries (private key and certificate) and export the two items to a `p12` file again by right clicking on them
 - Use the content of the `p12` file for GitHub repository secret `APPLE_CERT_CONTENT` (`base64 -i Certificates.p12`)
 - Use the password of the `p12` file for GitHub repository secret `APPLE_CERT_PASSWORD`

Create provisioning profiles:
 - XCode will register the app identifiers automatically for this project, both for the main app (`at.asitplus.wallet.compose`) and the `IdentityDocumentProviderExtension` target (`at.asitplus.wallet.compose.IdentityDocumentProviderExtension`)
 - Make sure the extension's App ID has the `App Groups` capability enabled and assigned to `group.Compose.Wallet`
 - Create the following provisioning profiles on the [Apple developer website](https://developer.apple.com/account/resources/profiles/add):
   - `iOS App Development` for the main app, name it `Compose Wallet Development`
   - `App Store Connect` for the main app, name it `Compose Wallet Distribution`
   - `App Store Connect` for the extension app identifier, name it `Compose Wallet Extension Distribution`
 - If you create a new key and certificate (above), you can edit the existing provisioning profile to add the new certificate
 - Be sure to include the necessary entitlements, e.g. associated domains
 - Download the provisioning profiles in XCode and set them for the project, instead of `automatically manage signing` (you can also download the profile from the website and import it in XCode when selecting the profile for the release build)
 - Use `Compose Wallet Development` for debug builds
 - Use `Compose Wallet Distribution` (main app) and `Compose Wallet Extension Distribution` (extension) for release builds

Local Fastlane release builds for internal users:
 - Copy `iosApp/Configuration/Signing.example.xcconfig` to `iosApp/Configuration/Signing.local.xcconfig`
 - Set `SIGNING_DEVELOPMENT_TEAM = 9CYHJNG644`
 - Set `SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp.entitlements` if you also need local Debug device builds with NFC and associated domains
 - Set `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Distribution`
 - Set `SIGNING_RELEASE_EXTENSION_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Extension Distribution`
 - Place the exported signing certificate at `iosApp/cert.p12`
 - Export `APPLE_CERT_PASSWORD`
 - Export `APPLE_ID`, `APPLE_API_KEY_ID`, `APPLE_API_ISSUER_ID`, and `APPLE_API_KEY_CONTENT`
 - Run `cd iosApp && fastlane build` for a release build or `cd iosApp && fastlane deploy` to upload to TestFlight

Required secrets for GitHub Actions:
- `APPLE_ID` with your Apple Development mail address
- `APPLE_API_KEY_ID` with the key id from your App Store Connect API Key
- `APPLE_API_ISSUER_ID` with the issuer id from your App Store Connect API Key
- `APPLE_API_KEY_CONTENT` with the Base64-encoded content of the `p8` file from the App Store Connect API Key
- `APPLE_CERT_CONTENT` with the Base64-encoded content of the `p12` certificate you've exported
- `APPLE_CERT_PASSWORD` with the password of the certificate you've exported
- `APPLE_DEVELOPMENT_TEAM` wtih the Apple Team ID

### Android: APKs, internal testing and promotion to open testing

Run **Publish Android App** (`.github/workflows/publish-android.yml`) manually from GitHub Actions. Gradle builds debug and release APKs and a release Android App Bundle (`.aab`), and the workflow saves all three as artifacts. It then runs `bundle exec fastlane android deploy` from `androidApp/` to upload the bundle to Google Play's **internal testing** track (`track: "internal"` in the Android Fastfile). The lane in `androidApp/fastlane/Fastfile` uses Fastlane's built-in [`upload_to_play_store`](https://docs.fastlane.tools/actions/upload_to_play_store/) action. The release is rolled out to the configured internal testers (`release_status: "completed"`, up to 100 testers). Promotion to open testing is a separate manual step; the lane does not publish to open testing or production. Store metadata, release notes, images and screenshots are managed in Play Console and are not uploaded by this lane.

Open testing lets anyone join through the opt-in link, similar to a TestFlight public link, and also makes the beta visible on Google Play. It does not require an email allowlist or Google Group. Closed testing would restrict enrollment to an approved list or group even if its opt-in link were shared publicly. Google's Developer API calls the open testing track [`beta`](https://developers.google.com/android-publisher/tracks#track_name_for_form_factor_tracks).

After testing internally, promote the release to **Open testing** in Play Console, or create an open testing release and use **Add from library** to select the already uploaded bundle. This reuses the exact same build and `versionCode`: do not rebuild, re-upload, or increment the version code just to promote it. Complete the open testing setup and any required review before rolling it out. See Google's [release guide](https://support.google.com/googleplay/android-developer/answer/9859348).

#### Ruby and Android build hosts

Android publishing reuses `iosApp/Gemfile` and `iosApp/Gemfile.lock`, including the same pinned Fastlane version as iOS. `BUNDLE_GEMFILE` selects that shared dependency definition; the Android lane has its own Fastfile and does not load the iOS signing lanes. The lockfile includes Linux and generic Ruby platforms alongside macOS.

The workflow uses `ruby/setup-ruby@v1` with Ruby **3.3** and `bundler-cache: true`. It installs the lockfile's Bundler version (currently **2.4.10**), runs `bundle install`, and caches the gems. There is no separate `gem install fastlane` step.

* **GitHub-hosted Ubuntu runners:** Ruby and gems are provisioned by the action; no manual host setup is needed.
* **Self-hosted Linux runners** (used when this repository is private): provision the existing Android/JDK build prerequisites plus Ruby's runtime libraries and a C/C++ build toolchain for native gems. A host matching a supported GitHub runner image can use the action's prebuilt Ruby; it needs matching OpenSSL libraries, libyaml and libgmp, with `/opt/hostedtoolcache` and `/home/runner` writable by the runner account. On other distributions, install Ruby 3.3 in the runner tool cache as instructed by `ruby/setup-ruby` when it detects an unsupported host. A system `ruby` or `gem` on `PATH` alone is not sufficient for the action's tool-cache lookup. The runner also needs network access to download Ruby, Bundler and gems and to call the Google Play API. Install OS packages/tool-cache Ruby while provisioning the host, rather than using `sudo` during publishing.

The Ruby installation must include **RubyGems and development headers**. Source-built and action-provided Rubies normally include the headers; packaged Ruby may need a matching `ruby-dev`/`ruby-devel` package. Bundler compiles native extensions for gems such as `json`, `bigdecimal` and `nkf`; having only the Ruby interpreter is not enough.

See [`ruby/setup-ruby` self-hosted runner requirements](https://github.com/ruby/setup-ruby#using-self-hosted-runners). No Ruby installation is needed for Android workflows that only build/test through Gradle.

For local uploads, install Ruby 3.3 with RubyGems and Bundler, then run from the repository root:

```bash
export BUNDLE_GEMFILE="$PWD/iosApp/Gemfile"
gem install bundler -v 2.4.10
bundle install
./gradlew :androidApp:bundleRelease
# Set GOOGLE_PLAY_SERVICE_ACCOUNT_JSON to the raw JSON key contents securely.
cd androidApp
bundle exec fastlane android deploy
```

The upload lane expects the existing release bundle at `androidApp/build/outputs/bundle/release/androidApp-release.aab`; it does not build or sign it. The local Gradle build also needs `android.cert.password` or `ANDROID_CERT_PASSWORD` as described above.

#### One-time Play Console and signing setup

1. In the team's [Google Play Console](https://play.google.com/console), create Valera with package name `at.asitplus.wallet.app.android`. Under **Testing > Internal testing > Testers**, configure the tester list, feedback URL/email and opt-in link. Before later promoting a release to open testing, complete the required public store listing, policy declarations (including Data safety), and country availability. Under **Testing > Open testing > Testers**, select the tester limit (or unlimited), set a feedback URL/email, and copy the public opt-in link to share on `wallet.a-sit.plus`. Open testing must be available for the developer account; complete any account-specific prerequisites shown by Play Console first.
2. Configure **Play App Signing** to **provide your own app signing key**, using the existing key that signs the APKs distributed on `wallet.a-sit.plus`. Follow Play Console's encrypted key export/upload instructions (PEPK). Retain the local key: it continues signing website APKs, while Google receives a copy for signing Play-delivered APKs. Check that the Play app signing certificate's SHA-256 fingerprint matches the certificate of the website release APK. Accepting a new Google-generated key instead would give the two distributions different signing identities.
3. Check the existing signing certificate before enrollment. Google Play requires its validity to end after **22 October 2033**. The 365-day certificate command in **Local deployments** above is for local debug builds and does not meet this requirement. Do not generate a replacement release key just to add Play distribution.
4. The current Gradle configuration signs both APKs and the release bundle with `androidApp/keystore.p12`, alias `key0`. Register that same key's certificate as the **upload certificate** in Play Console as well. Google permits using one key for app signing and uploads; a separate upload key is recommended by Google, but would require a separate bundle signing configuration in this project.
5. Build the first release bundle locally with `./gradlew :androidApp:bundleRelease` using the existing signing configuration, or download it from the workflow's **Wallet App Android (Google Play)** artifact. Upload it manually in Play Console and roll out the first internal testing release before using automated publishing. Artifacts are uploaded before the Play upload step, so they remain available if that step fails during initial setup. Finish any required declarations/review steps in Play Console. Testers must opt out of internal testing before opting into open testing.

Keep `applicationId` and the app signing certificate consistent across website and Play builds. Increase `version.code` in `gradle.properties` for every new build uploaded to Play, including repeat workflow runs after a successful upload; Play does not allow uploading another bundle with an already used version code. Promoting the existing bundle between tracks retains its version code. Coordinate version codes across both distribution channels so updates can move forward without uninstalling the app.

See Google's [Play App Signing guide](https://support.google.com/googleplay/android-developer/answer/9842756), [certificate requirements](https://developer.android.com/studio/publish/app-signing#considerations), and [testing-track setup](https://support.google.com/googleplay/android-developer/answer/9845334).

#### Google Play API credentials

1. Create or select a team-owned project in [Google Cloud Console](https://console.cloud.google.com/) and enable the [Google Play Android Developer API](https://console.cloud.google.com/apis/library/androidpublisher.googleapis.com).
2. Under **IAM & Admin > Service Accounts**, create a service account for GitHub publishing. It does not need a Google Cloud project role for Play publishing; its app access is granted in Play Console.
3. In **Play Console > Users and permissions**, invite the service account's email address. Grant access to Valera only, with **View app information (read-only)** and **Release apps to testing tracks**. Tester lists and open testing enrollment settings are managed manually in Play Console; the workflow does not need production-release or tester-management permissions.
4. In Google Cloud Console, open that service account's **Keys > Add key > Create new key > JSON** and download the JSON file. Store its complete, raw JSON contents in the GitHub secret below; do not Base64-encode it or commit it.

See Google's [Developer API setup](https://developers.google.com/android-publisher/getting_started) and [Play Console permission definitions](https://support.google.com/googleplay/android-developer/answer/9844686).

#### GitHub Actions secrets

Set these under the GitHub repository's **Settings > Secrets and variables > Actions > Repository secrets** (or provide organization secrets with access to this repository):

| Secret | Value and source |
| --- | --- |
| `ANDROID_CERT_PASSWORD` | Existing password for `androidApp/keystore.p12` and its `key0` entry, obtained from the maintainer responsible for the website APK signing key. The workflow passes it to Gradle. Keep the existing value if it is already configured. |
| `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON` | Complete JSON key file downloaded from **Google Cloud Console > IAM & Admin > Service Accounts > the publishing account > Keys**. Used to authenticate the upload to Play. |

The existing workflow uses the repository's checked-in Android keystore; no additional keystore secret is introduced here. Do not commit new keys, passwords, or service-account JSON files. No Google account password, developer-account ID secret, or OAuth client secret is needed.

## DC API matcher

Android uses custom WebAssembly matchers for the Digital Credentials API integration.
These matchers are loaded from app assets by [`CustomRegistry`](./shared/src/androidMain/kotlin/at/asitplus/wallet/app/android/dcapi/CustomRegistry.kt).

There are currently two separate matcher binaries:
* Verification matcher: `androidApp/src/main/assets/dcapimatcher.wasm`
* Issuance matcher: `androidApp/src/main/assets/dcapimatcher_issuing_hardcoded.wasm`

Source locations:
* Verification matcher: [`shared/src/androidMain/kotlin/matcher`](./shared/src/androidMain/kotlin/matcher)
* Issuance matcher: [`shared/src/androidMain/kotlin/matcher_issuance_hardcoded`](./shared/src/androidMain/kotlin/matcher_issuance_hardcoded)
* Build instructions:
  [`shared/src/androidMain/kotlin/matcher/README.md`](./shared/src/androidMain/kotlin/matcher/README.md)
  and
  [`shared/src/androidMain/kotlin/matcher_issuance_hardcoded/README.md`](./shared/src/androidMain/kotlin/matcher_issuance_hardcoded/README.md)

Prerequisites:
* Install [WASI SDK](https://github.com/WebAssembly/wasi-sdk/releases), version 20
* The Makefiles assume it is available under `~/wasi-sdk-20.0`

Notes:
* The verification matcher parses DCQL requests in native code, mainly in [`dcql.cpp`](./shared/src/androidMain/kotlin/matcher/dcql.cpp) and [`Request.cpp`](./shared/src/androidMain/kotlin/matcher/Request.cpp).
* If you change the native matcher sources, rebuild the `.wasm` binary and copy it into the Android assets, otherwise the app will continue using the previously bundled matcher.
* Complex DCQL requests are only supported to the extent implemented in the matcher and UI.

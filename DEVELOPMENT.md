# Compose Multiplatform Wallet App

## Development

Development happens in branch `development`. The `main` branch always tracks the latest release. Hence, create PRs against `development`. Use dedicated `release/x.y.z` branches to prepare releases and create release PRs against `main`, which will then be merged back into `development`.

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
* Public Debug builds default to reduced entitlements, so `Associated Domains` and NFC are not available unless you are using internal signing.
* Public contributors cannot run the app on a physical iPhone with the official bundle identifier `at.asitplus.wallet.compose`. Apple requires that bundle identifier to be signed by the team that owns it.
* Device builds and release archives for the official app identifier therefore require access to the A-SIT Apple Developer team.

Internal team members:
* Device builds use Xcode automatic signing in the `Debug` configuration.
* Copy [iosApp/Configuration/Signing.example.xcconfig](iosApp/Configuration/Signing.example.xcconfig) to `iosApp/Configuration/Signing.local.xcconfig`.
* Set `SIGNING_DEVELOPMENT_TEAM` in `Signing.local.xcconfig`.
* Set `SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp/iosApp.entitlements` if you want `Associated Domains` and NFC in local Debug device builds.
* Set `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER` in `Signing.local.xcconfig` if you want to create release archives locally.

Minimal `Signing.local.xcconfig` for internal users:

```xcconfig
APP_BUNDLE_IDENTIFIER = at.asitplus.wallet.compose
SIGNING_DEVELOPMENT_TEAM = 9CYHJNG644
SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp/iosApp.entitlements
SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Distribution
```

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
* The `Release` configuration uses manual signing.
* Maintainers can provide `SIGNING_DEVELOPMENT_TEAM` and `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER` in the untracked `iosApp/Configuration/Signing.local.xcconfig`.
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
 - XCode will register the app identifier automatically for this project
 - Create two provisioning profiles on the [Apple developer website](https://developer.apple.com/account/resources/profiles/add), one for `iOS App Development` (name it `Compose Wallet Development`) and one for `App Store Connect` (name it `Compose Wallet Distribution`)
 - If you create a new key and certificate (above), you can edit the existing provisioning profile to add the new certificate
 - Be sure to include the necessary entitlements, e.g. associated domains
 - Download the provisioning profiles in XCode and set them for the project, instead of `automatically manage signing` (you can also download the profile from the website and import it in XCode when selecting the profile for the release build)
 - Use `Compose Wallet Development` for debug builds
 - Use `Compose Wallet Distribution` for release builds

Local Fastlane release builds for internal users:
 - Copy `iosApp/Configuration/Signing.example.xcconfig` to `iosApp/Configuration/Signing.local.xcconfig`
 - Set `SIGNING_DEVELOPMENT_TEAM = 9CYHJNG644`
 - Set `SIGNING_DEBUG_ENTITLEMENTS_FILE = iosApp/iosApp/iosApp.entitlements` if you also need local Debug device builds with NFC and associated domains
 - Set `SIGNING_RELEASE_PROVISIONING_PROFILE_SPECIFIER = Compose Wallet Distribution`
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

For Android we use a keystore to sign and build the app. The keystore is checked in, the password needs to be set as a environment variable.

Setup:
 - Create a new keystore, e.g. from Android Studio

Required secrets for GitHub Actions:
 - `ANDROID_CERT_PASSWORD` with the password for the keystore you've exported

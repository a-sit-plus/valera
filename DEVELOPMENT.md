# Compose Multiplatform Wallet App

## Development

Development happens in branch `development`. The `main` branch always tracks the latest release. Hence, create PRs against `development`. Use dedicated `release/x.y.z` branches to prepare releases and create release PRs against `main`, which will then be merged back into `development`.

## Local deployments

To sign the Android App with the correct signer certificate (used for Key Attestation checks on the backend), add the property `android.cert.password` with the correct password to your `local.properties`. For CI deployments, see below.

## Deployments

We use [fastlane](https://fastlane.tools/) to build the iOS App. The CI pipeline and secrets on this GitHub repository are already set up correctly. No need to do it again!

Setup:
 - Get an Apple Development Account
 - Create an [App Store Connect API Key](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api) (with `App manager` access) and download it
 
Create a new certificate:
 - On your Mac, go to Keychain Access
 - Under Certificates, select the Apple Worldwide Developer Relations Certification Authority
 - In the menu go to Certificate Assistant, Request a Certificate from a Certificate Authority
 - There, enter your mail address, and set "Valera" as the Common Name, and save the CSR to disk
 - On the [Apple developer website](https://developer.apple.com/account/resources/certificates/add), create a certificate for `Apple development` and one for `Apple distribution` with the CSR generated before
 - Import the generated certificates into Keychain Access, to associate them with your key,
 - Select both entries (private key and certificate) and export the two items to a `p12` file again by right clicking on them
 - Use the content of the `p12` file for `APPLE_CERT_CONTENT`
 - Use the password of the `p12` file for `APPLE_CERT_PASSWORD`

Create provisioning profiles:
 - XCode will register the app identifier automatically for this project
 - Create two provisioning profiles on the [Apple developer website](https://developer.apple.com/account/resources/profiles/add), one for `iOS App Development` (name it `Compose Wallet Development`) and one for `App Store Connect` (name it `Compose Wallet Distribution`)
 - Be sure to include the necessary entitlements, e.g. associated domains
 - Download the provisioning profiles in XCode and set them for the project, instead of `automatically manage signing`
 - Use `Compose Wallet Development` for debug builds
 - Use `Compose Wallet Distribution` for release builds

Required secrets for GitHub Actions:
- `APPLE_ID` with your Apple Development mail address
- `APPLE_API_KEY_ID` with the key id from your App Store Connect API Key
- `APPLE_API_ISSUER_ID` with the issuer id from your App Store Connect API Key
- `APPLE_API_KEY_CONTENT` with the Base64-encoded content of the `p8` file from the App Store Connect API Key
- `APPLE_CERT_CONTENT` with the Base64-encoded content of the `p12` certificate you've exported
- `APPLE_CERT_PASSWORD` with the password of the certificate you've exported

For Android we use a keystore to sign and build the app. The keystore is checked in, the password needs to be set as a environment variable.

Setup:
 - Create a new keystore, e.g. from Android Studio

Required secrets for GitHub Actions:
 - `ANDROID_CERT_PASSWORD` with the password for the keystore you've exported

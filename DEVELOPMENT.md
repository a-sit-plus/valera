# Compose Multiplatform Wallet App

## Local deployments

To sign the Android App with the correct signer certificate (used for Key Attestation checks on the backend), add the property `android.cert.password` with the correct password to your `local.properties`. For CI deployments, see below.

## Deployments

We use [fastlane](https://fastlane.tools/) to build the iOS App. The CI pipeline and secrets on this GitHub repository are already set up correctly. No need to do it again!

Setup:
 - Get an Apple Development Account
 - Create an [App Store Connect API Key](https://developer.apple.com/documentation/appstoreconnectapi/creating_api_keys_for_app_store_connect_api) and download it
 - Run `cd iosApp; fastlane createCert` to create a new signing certificate and provisioning profile
 - Export the new certificate from your local keychain

Required secrets for GitHub Actions:
- `APPLE_ID` with your Apple Development mail address
- `APPLE_API_KEY_ID` with the key id from your App Store Connect API Key
- `APPLE_API_ISSUER_ID` with the issuer id from your App Store Connect API Key
- `APPLE_API_KEY_CONTENT` with the Base64-encoded content of the `p8` file from the App Store Connect API Key
- `APPLE_CERT_CONTENT` with the Base64-encoded content of the `p12` certificate you've exported
- `APPLE_CERT_PASSWORD` with the password of the certificate you've exported

For Android we use a keystore to sign and build the app.

Setup:
 - Create a new keystore, e.g. from Android Studio

Required secrets for GitHub Actions:
 - `ANDROID_CERT_CONTENT` with the Base64-encoded content of the `p12` keystore you've created
 - `ANDROID_CERT_PASSWORD` with the password for the keystore you've exported

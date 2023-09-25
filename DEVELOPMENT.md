# Compose Multiplatform Wallet App

## Deployments

We use [fastlane](https://fastlane.tools/) to build the iOS App.

Required secrets for GitHub Actions:
- `APPLE_ID` with your Apple Development mail address
- `APPLE_SIGNING_KEY_STORE` to hold the base64 encoded keystore with the distribution certificate
- `APPLE_SIGNING_KEY_PASSWORD` to hold the password for the keystore

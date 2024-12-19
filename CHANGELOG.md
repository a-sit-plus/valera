# Release NEXT
 * Claim more custom URL schemes: `haip`, `eudi-openid4vp`, `mdoc-openid4vp`
 * Claim URL of Remote Wallet: `https://wallet.a-sit.at/remote/`

# Release 5.4.0
 * Rebrand to Valera
 * Kotlin 2.1.0
 * Update to vck 5.2.0
 * Choose which credentials and attributes to actually send during authentication

# Release 5.3.0
 * Update to vck 5.1.0
 * Update to Compose Multiplatform 1.7.0, refactoring views and viewmodels
 * Split up issuing process to get metadata first, support transaction code

# Release 5.2.0
 * Display requested attributes on Consent screen
 * Enable provisioning by scanning QR Code on issiung service (OID4VCI pre-authorized flow)

# Release 5.1.0
 * Parse `redirect_uri` in response to posting SIOPv2 response to service provider, may open browser
 * Always open browser when sending response to SIOPv2 calls
 * Jump into browser for general issuing process

# Release 5.0.0
* Dependency updates to VC-K 5.0.0

# Release 4.2.0
* Dependency updates to VC-K 4.2.0-SNAPSHOT
* Remove custom crypto code and rely on Supreme Crypto Provider

# Release 4.1.1:
* handle image decoding error

# Release 4.1.0:
* improved display of credentials
* Handle portraits encoded as actual byte arrays

# Release 4.0.0:
* Update to VcLib 4.1.1
* Add credentials: POR, COR
* Add FaceId support for iOS
* Remove mutli-authorization on signing the presentation

# Release 2.0.3:
 * Fix loading of SD-JWT credentials

# Release 2.0.2:
 * iOS Signature

# Release 2.0.1:
 * Fix serialization issues

# Release 2.0.0:
 * Support vclib 3.8.1

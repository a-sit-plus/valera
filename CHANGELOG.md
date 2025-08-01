# Release 5.7.3
 * Update to VC-K 5.8.0, fixing optional attributes during presentation
 * Credentials: Add FallbackCredentialScheme for unknown schemes
 * Improve display of complex credentials in technical detail view
 * Add SessionService to handle scoped Koin objects
 * Add FallBackKeyMaterial to catch startup exceptions
 * Remove `getMatchingCredentials` (move to vck)

# Release 5.7.2 
 * Proximity: Use fixed IACA key and certificate for reader authentication
 * Credentials: Show technical metadata (validity, status)

# Release 5.7.1
 * Update to VC-K 5.7.1, fixing JWS certificate encoding

# Release 5.7.0
 * Require biometric auth to use holder key
 * Update to VC-K 5.7.0, adding encryption and key agreement for all targets
 * Add support for OID4VP and ISO 18013-7 Annex C over the Digital Credentials API
 * Show Credential Freshness

# Release 5.6.5
 * Credentials: Safe decoding of images in credentials
 * Credentials: Update EHIC to latest schema (1.1.0)

# Release 5.6.4
 * Add: Koin Dependency Injection
 * Issuing: Fix regression not storing credentials in auth code flow
 * Presentation: Treat all requested attributes as optional by default on consent screen
 * Upgrade to VC-K 5.6.5 (improving interop)

# Release 5.6.3
 * Set iOS target of app to 16.0
 * Presentation: Improve handling queries with nested paths
 * Issuing: Do not use deprecated code from VC-K 5.6.3
 * Upgrade to VC-K 5.6.4 (improving OpenID4VCI interop)

# Release 5.6.2
 * Issuing: Handle errors on parsing credential offers
 * Proximity: Support requesting credentials aside from PID, mDL
 * Upgrade to VC-K 5.6.3 (improving OpenID4VCI interop)

# Release 5.6.1
 * Upgrade to VC-K 5.6.2 (sending `state` parameter even with `direct_post.jwt` to verifiers)
 * mDL: Fix german localization for some claims
 * PID: Support presenting all claims
 * Improve error messages
 * Add: Caching of status list tokens
 * Proximity: Persist transfer settings

# Release 5.6.0
 * Upgrade to VC-K 5.6.0
 * Display credential validity for invalid credentials
 * Update german localizations
 * Display validity of loaded credentials
 * Gracefully handle the case where the token status cannot be evaluated
 * Improve error messages
 * Add proximity presentation and verification of ISO credenitals over Bluetooth
 * Add option to open URL from verifier after successful authentication
 * Add support for EU PID with SD-JWT names from ARF 1.8.0 onwards
 * Add support for more age over claims in EU PID and mDL
 * Update Tax ID credential, to fix `sdJwtType`

# Release 5.5.2
 * Fix presentation of ISO credentials

# Release 5.5.1
 * Fix DCQL Query extraction for SD-JWT
 * Support more credential identifiers for presentation exchange
 * Update to VC-K 5.5.1

# Release 5.5.0
 * Android: Add NFC device engagement/retrieval and Bluetooth device retrieval mechanisms
 * Update to VC-K 5.5.0: Use OID4VCI Draft 15 for loading credentials
 * Please migrate to issuing service at <https://wallet.a-sit.at/m6>

# Release 5.4.16
 * Recognize DCQL queries for consent preview and show dummy consent screen
 * Attribute Selection for DCQL queries

# Release 5.4.15
 * Update Power of Representation credential, to fix `sdJwtType`
 * Update Company Registration credential, to fix `sdJwtType`

# Release 5.4.14
 * Improve consent: Mark optional attributes
 * Rework start screen

# Release 5.4.13
 * Update EU PID to ARF 1.5.0
 * Remove selecting single attributes for issuing (has been removed from OID4VCI)

# Release 5.4.12
 * Replace ePrescription with HealthID
 * Add Remote Qualified Electronic Signatures (RQES), acc. to UC5 in POTENTIAL
 * Update to VC-K 5.4.4

# Release 5.4.11
 * Update to VC-K 5.4.2
 * Initial Tax ID Credential Support

# Release 5.4.10
 * Update to VC-K 5.3.3 to fix ISO mdoc presentation acc. to ISO 18013-7
 * EU PID: Parse gender as integer too

# Release 5.4.9
 * Update to VC-K 5.3.2 to fix ISO value digests

# Release 5.4.8
 * Support more custom URL schemes: `openid4vp` and `openid-credential-offer`

# Release 5.4.7
 * Use ISO names for EU PID again

# Release 5.4.6
* Update to EU PID 2.3.2 to show portrait
* Update to CoR 2.1.2 to show more address claims
* Update to VC-K 5.3.1 to fix AES GCM encryption of responses

# Release 5.4.5
 * Update to VC-K 5.3.0 to support encryption for mDoc responses acc. to ISO 18013-7
 * Add update check on app start

# Release 5.4.4
 * Support [Company Registration Credential](https://github.com/a-sit-plus/company-registration-credential/)
 * Update to VC-K 5.2.3 to increase interoperability with Verifier implementations
 * Support more `age_over_NN` attributes for EU PID and mDL

# Release 5.4.3
* Authentication Selection
* UI Improvements
  * Add A-SIT Logo to Views
  * ePrescription Icon Label
  * Use card for AttributeSelectionGroup
* Fix crash during provisioning
* Fix Deselection of attributes being ignored
* VC-K 5.2.2
* mDL 1.1.3


# Release 5.4.2
 * Go back to vck 5.2.1 (still fixing ISO mDoc presentations)

# Release 5.4.1
 * Claim more custom URL schemes: `haip`, `eudi-openid4vp`, `mdoc-openid4vp`
 * Claim URL of Remote Wallet: `https://wallet.a-sit.at/remote/`
 * Update to VC-K 5.2.1, fixing ISO mDoc presentations
 * Update all Credentials to base them on VC-K 5.2.1
 * Remove last build tool bugs workaround
 * Update to latest conventions, fixing XCF export mess in build script
 * Add logo of A-SIT Plus GmbH to headers of all Screens

# Release 5.4.0
 * Rebrand to Valera
 * Kotlin 2.1.0
 * Update to VC-K 5.2.0
 * Choose which credentials and attributes to actually send during authentication

# Release 5.3.0
 * Update to VC-K 5.1.0
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

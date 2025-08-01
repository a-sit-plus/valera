# Release 5.7.5 (unreleased)

# Release 5.7.4
 * Presentation:
   * Support URL scheme `haip-vp` from latest HAIP draft
   * Always show requested attributes
   * Update to [VC-K 5.10.0](https://github.com/a-sit-plus/vck/releases/tag/5.10.0), updating OpenID4VP to 1.0
   * Ensure compatibility with relying party at <https://apps.egiz.gv.at/eudi_rp/>
 * Issuing:
   * Support URL scheme `haip-vci` from latest HAIP draft
   * Support direct invocation from browser
   * Update to [VC-K 5.10.0](https://github.com/a-sit-plus/vck/releases/tag/5.10.0), updating OpenID4VCI to 1.0
   * Please migrate to issuing service at <https://wallet.a-sit.at/m7>
 * DC API:
   * Remove preview protocol
   * Update matcher
 * Credentials:
   * Update PIDs to rulebook from October 2025
   * Add [Age Verification](https://github.com/a-sit-plus/age-verification) credential
 * Proximity:
   * Update to [multipaz 0.94.0](https://github.com/openwallet-foundation/multipaz/releases/tag/0.94.0)
   * Add BLE and NFC capability checks for proximity (enabled, selected)
 * General:
   * Move transfer options selection from general settings view to proximity holder and verifier route
   * Add CapabilitiesService, CapabilitiesView to hint wrong settings
   * Add InitializationView
   * Add AuthCheckKit as new dependency
   * Build: Set `disableAppleTargets=true` in `local.properties` to disable iOS targets on non-Mac hosts

# Release 5.7.3
 * Update to VC-K 5.8.0, fixing optional attributes during presentation
 * Credentials: Add FallbackCredentialScheme for unknown schemes
 * Improve display of complex credentials in technical detail view
 * Add SessionService to handle scoped Koin objects
 * Add FallBackKeyMaterial to catch startup exceptions
 * Remove `getMatchingCredentials` (move to VC-K)

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

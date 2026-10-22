# Release 5.9.1 (unreleased):
 * Presentation: Parse and validate incoming requests, including certificate trust checks, off the UI thread for DC API, OpenID4VP, and proximity flows
 * Authentication: Use “Authenticate to continue” consistently for credential issuance, refresh, and presentation prompts on Android and iOS
 * Update to VC-K 9.0.0-SNAPSHOT
 * Presentation: Resolve and display DCQL `mso_mdoc_zk` credential queries on the consent screens like ISO mdoc queries
 * Presentation: Show a badge on the consent screens stating whether an ISO mdoc is requested as a plain mdoc or as a zero-knowledge proof (with or without plain fallback)
 * Presentation: Show why the WRPAC or WRPRC of a relying party is invalid, e.g. because the WRPRC could not be parsed, is revoked or suspended, its status could not be checked, or it is not linked to a valid WRPAC, and evaluate each of them even if the other one is invalid. A WRPAC or WRPRC missing from the request is shown as not provided instead of invalid
 * Issuing and presentation: Use VC-K’s public Ktor clients with Valera’s configured HTTP clients
 * Issuing: Use the client-attestation key for DPoP when wallet attestation is enabled
 * Issuing: Validate issued credentials with VC-K's `CredentialValidator` before storing them, which checks their integrity as before, and trust in their issuers per credential type from the Lists of Trusted Entities
 * Presentation: Validate requests and their relying parties with VC-K's validation reports, and show the outcome as a warning with every check of the request, its access certificate, and its registration certificates, e.g. an untrusted, expired, revoked, or unlinked certificate, or requested data that is not registered; requests failing their integrity, e.g. their signature, are still rejected
 * Issuing: Request signed issuer metadata and show the validation of the issuer before issuance, i.e. whether its access certificate is trusted, its registration information and registration certificate, and whether it is registered for the offered credential; untrusted issuers are shown as a warning, unsigned metadata as not verified
 * Issuing: Ask before storing an issued credential that did not pass every check, e.g. one of an untrusted issuer, an expired one, or one whose status could not be checked, show why, and store it only if the user keeps it
 * Issuing: Check that an issued credential contains every claim the metadata of its type describe as mandatory, and ask before storing one that misses some, naming the claims the issuer left out; the user may still keep it
 * Issuing: Validate each received credential once as VC-K decodes it from the credential response, so that an ISO mdoc is stored with exactly the bytes the issuer signed, and reject a credential that can not be decoded as failing its parsing
 * Issuing: Check the status of an issued credential with its status list fetched again, replacing a cached one, instead of a status list cached for up to its time to live, also while a recent error response of its URL would suppress fetching it
 * Issuing: Select the credentials of an offer with a checkbox each instead of a dropdown, showing the scheme and representation of every offered credential, and whether the issuer is not registered for it; none is selected at first, so that the user selects each one to request explicitly, and all selected ones are requested with one authorization
 * Issuing: Show every credential issued at once on the result screen, with a card each, instead of only the first one
 * Proximity: Verify the reader authentication of requests over BLE and NFC, reject an invalid one, and show the validation of the reader and its registration certificates like for other relying parties
 * Proximity verifier: Sign the reader authentication over the session transcript itself instead of its encoding as a byte string, which holders rejected as an invalid signature
 * Proximity verifier: Validate received documents with VC-K's `CredentialValidator`, which now also verifies their device signature or device MAC over the session transcript and trusts their issuers per credential type from the Lists of Trusted Entities, and show the validation of each document with every check; untrusted, expired, or revoked documents are shown with a warning, the claims of documents failing their integrity, e.g. their signature or digests, are not shown
 * Presentation: Warn about a request signed with a certificate that is no access certificate (WRPAC), i.e. carries no identifier of a relying party, instead of rejecting it
 * DC API: Validate the reader authentication of ISO mdoc requests without registration certificates, reject an invalid one, and show the validation of the reader like for other relying parties; on iOS, show the trust status of the reader on the first screen of the extension as preliminary, and the full validation after continuing, refreshing missing or expired Trust Lists for up to three seconds first
 * Credentials: Show the validation of a stored credential in its details, i.e. its signature, the trust in its issuer, its validity period, and its status, with every check
 * Credentials: Check the validity period and status of stored credentials with the validation of VC-K, which accepts a status only from status lists signed by a trusted signer, and shows other statuses as not checked
 * Credentials: Offer to refresh revoked credentials, not only expired ones, as their status is now checked in the background
 * Trust Lists: Load root certificates of registration certificate (WRPRC) providers from a bundled file and from a URL configurable in the settings
 * Trust Lists: Load the WRPRC trust anchors from the trusted certificates of the EUDI Wallets Launchpad 2026 by default, a ZIP archive whose folder “WRPRC Provider CA Certificates” holds the providers, and “WRPRC Revocation List Signing Certificates” the signers of their status lists, with PEM or DER encoded files; persist and refresh them in the background like the Lists of Trusted Entities
 * Settings: Add “Refresh trust lists”, which downloads every List of Trusted Entities and the WRPRC trust anchors again, and a button to restore the default WRPRC trust anchors URL
 * Proximity: Keep the Bluetooth connection open after sending the response until the reader closes it, for up to five seconds, so that the reader receives the response instead of failing with a closed connection
 * Proximity verifier: Honor the “Enable L2CAP in engagement” setting for NFC engagement, which used the general L2CAP setting instead
 * DC API: Match SD-JWT credentials on Android for DCQL claim paths with array indices or `null` wildcards, e.g. `["nationalities", null]` requested by the EUDI reference verifier, which matched no credential, and update the matcher to Multipaz `f7d8e684`; `values` of a claim with a `null` wildcard match if any selected element matches
 * Presentation: Show a loading screen while the selected credentials are sent to the relying party, and ignore repeated confirmations, which could submit a presentation twice
 * DC API: Open the credential selection on iOS only once after continuing, as a late second navigation could replace it while in use, and show a credential whose freshness or issuer trust can not be evaluated as not checked instead of ending the flow; log uncaught exceptions before the process terminates, and requests the system cancels
 * Trust Lists: Continue a presentation with the cached lists if refreshing them, or the WRPRC trust anchors, fails in any way, e.g. as a request failed, was cancelled by its client or is in its failure cooldown, which could end the DC API flow on iOS; lists that can not be read establish no trust, a status list that can not be fetched is reported as unknown, and the lists are parsed off the main thread
 * Trust Lists: Parse each persisted list once per process, one at a time, and only when it changed, instead of every list for every trust state shown and again on every change of any stored value, which exceeded the memory limit of the iOS Identity Document Provider extension, so that the system terminated it with the lists of several stages enabled
 * Trust Lists: Download each list once if “Refresh trust lists” and the background refresh run at the same time, e.g. after enabling a stage, which downloaded every list twice, and report an HTTP error of a list, e.g. 403, as such instead of failing to parse the error page as a list
 * DC API: Do not start the periodic checks of the stored credentials, of the Trust Lists and of the WRPRC trust anchors, nor load the ZK circuits ahead of time, in the iOS Identity Document Provider extension, to reduce its memory usage; its requests still refresh stale Trust Lists, and load the ZK circuits when a presentation that requests a zero-knowledge proof is finalized
 * Credentials: Decode the stored credentials again only if they changed, instead of on every change of any stored value, e.g. of the HTTP cache, which held a second copy of every credential, to reduce the memory usage of the iOS Identity Document Provider extension
 * Presentation: Load the ZK circuits only for requests that ask for a zero-knowledge proof, which took several megabytes of memory for every presentation, e.g. in the iOS Identity Document Provider extension
 * Logging: Log only the headers of HTTP requests and responses in release builds and in the iOS Identity Document Provider extension, as logging their bodies, e.g. of the Trust Lists, took megabytes of memory
 * UI: Reduce the images of a successful and a failed presentation to the size they are shown in, which took about 6 MB of memory each
 * DC API: Answer an issuance request as declined once the user acknowledges that loading the offer, issuing, or storing the credentials failed, which left the request without an answer
 * DC API: Keep ISO mdoc requests as received on Android and iOS, as their reader authentication failed to verify after re-encoding a request whose encoding differs from the one of VC-K, e.g. a `zkRequest` of the age verification verifier, which was rejected
 * Presentation: Name the failed signature as the reason for rejecting a request instead of the parsing of its access certificate, which is not checked without a valid signature
 * DC API: Present a credential selected by the system on Android only for the credential query it was selected for, whose claims the system showed, instead of once for every credential query it matches, e.g. twice for a PID requested by two alternative ISO mdoc queries; the matcher reports the query with each entry of the picker
 * DC API: Match SD-JWT claims on Android against their original object and array structure, including whole objects such as `place_of_birth`, nested wildcards and indices, and exact typed values; distinguish literal claim names containing dots, numbers or `null` from path operations, and rebuild registrations with the updated matcher format; as OpenID4VP 1.0, 7.1.1 specifies, a claim whose path selects an element of the wrong type does not match, while other options of `claim_sets` without it still may
 * DC API: Send an error to the caller and end the flow when the error screen of a failed presentation is confirmed, e.g. of a rejected request, on Android and iOS, where the Identity Document Provider extension stayed open; errors while sending a presentation are now reported to the caller as well, instead of only cancelling the request

# Release 5.9.0:
 * Issuing and presentation: Accept Launchpad `eu-eaa-offer` and `eu-eaap` links on Android and iOS
 * Issuing: Clear stored and cached wallet attestations when wallet attestation is disabled in settings
 * Build: Downgrade the Gradle wrapper to 9.6.1
 * Build: Upgrade to the 20260828 conventions plugin and AGP 9, migrate the shared module to the new Kotlin Multiplatform Android library plugin API, and move the Android app to AGP built-in Kotlin
 * Build: Drop unused Android dependencies from the shared module and declare the Android app's own entry-point dependencies explicitly
 * Update to VC-K 8.0.0
 * Update to Multipaz 0.101.0
 * Credentials: Move expensive credential-list preparation and trust evaluation off the UI thread
 * DC API: Register unknown credential schemes with fallback metadata and preserve claim comparison values
 * DC API: Update the Android verification matcher from upstream Multipaz
 * DC API: Improve request grouping, fixed selections, loading and error handling, and credential selection layout
 * DC API: Match requests without claim constraints and display their mandatory attributes
 * Local transfer: Replace the malformed Bluetooth graphic with the official Material icon
 * Presentation: Label the final confirmation action Submit
 * Display trust evaluation of the Relying Party
 * Trust Lists: Load the Lists of Trusted Entities of the development stage as well as the acceptance stage, let every stage (acceptance, development, production) be enabled separately in the settings, and drop the cached lists of a stage when it is disabled
 * Trust Lists: Remove disabled-stage cache entries atomically when multiple wallet sessions access the store
 * Presentation: Replace remaining Presentation Exchange flows with DCQL and ISO DeviceRequest, including proximity and ISO DC API requests
 * Presentation: Label and show the value of every attribute an ISO DeviceRequest discloses, on both the selection and the confirmation screen
 * Presentation: Keep every requested DCQL attribute visible even when its value cannot be rendered
 * Presentation: Mark the requested attributes a verifier intends to retain beyond the transaction
 * Add WRPAC and WRPRC validation
 * Presentation: Answer ISO mdoc zero-knowledge proof requests with the Longfellow-ZK backend of `vck-longfellow`
 * DC API: Validate WRPAC and WRPRC of ISO mdoc requests against the DC API session transcript

# Release 5.8.2:
 * Networking: Use explicit CIO and Darwin HTTP engines and make Android debug trust configuration compatible with CIO
 * Prevent TLS probe errors from producing false offline results

# Release 5.8.1:
 * Update to VC-K 7.0.0
 * Rework resolving credential schemes
 * Drop support for PID in VC JWT (which was never officially specified anyway)
 * DCAPI: Update matching implementation, improve iOS state handling
 * Fix opening second provisioning flow after aborting the first one
 * Add `TrustListService` for loading LoTE regularly in the background
 * Display trust evaluation of credentials
 * Cache remote lookups of trust lists and status lists
 * Improve dark theme
 * Improve UI performance

# Release 5.8.0
 * Update to VC-K 6.0.0
 * Credentials: Show credential titles in refresh prompts and allow suppressing a prompt for a single credential
 * Issuing: Update key attestation to VC-K 6.0 / TS3 WUA 1.5 APIs
 * Raise iOS target from 16.0 to 18.6 (iOS 18 is the oldest version to still receive security updates)
 * Please migrate to issuing service at <https://wallet-issuer.a-sit.plus/>
 * Ensure compatibility with relying party at <https://wallet-rp.a-sit.plus/>
 * DC API:
   * Support requests with multiple protocols
   * Use VC-K data classes to (de-)serialize received/emitted data
   * Add support for iOS using ISO/IEC 18013-7 Annex C protocol
   * Add support for issuance via the DC API based on the preliminary spec defined in https://github.com/openid/OpenID4VCI/pull/476
   * Add a configurable OpenID4VP origin-scheme allowlist; debug builds additionally allow `http` by default

# Release 5.7.6
 * Update to VC-K 5.12.0
 * Credentials: Fix displaying age verification credential
 * Credentials: Remove support for ID Austria credential
 * Support URL scheme `av` for age verification
 * DC API: Use VC-K serializers
 * DC API: Support requests with multiple protocols
 * Add: Support refreshing of credentials
 * Add: Support presentation for general DCQL queries
 * Add: Debug version allows for localhost cleartext transmissions, supporting local test environments

# Release 5.7.5
 * Update to VC-K 5.10.1
 * Provide self-signed key attestation on issuing

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

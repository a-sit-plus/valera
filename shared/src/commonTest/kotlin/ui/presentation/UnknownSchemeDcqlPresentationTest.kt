package ui.presentation

import at.asitplus.KmmResult
import at.asitplus.iso.IssuerSignedItem
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.openid.OpenIdConstants.ResponseMode
import at.asitplus.openid.dcql.DCQLClaimsPathPointer
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.RequestOptionsCredential
import at.asitplus.wallet.lib.agent.ClaimToBeIssued
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.Validator
import at.asitplus.wallet.lib.agent.Verifier
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.ISO_MDOC
import at.asitplus.wallet.lib.data.ConstantIndex.CredentialRepresentation.SD_JWT
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialScheme
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.AuthnResponseResult
import at.asitplus.wallet.lib.openid.AuthorizationResponsePreparationState
import at.asitplus.wallet.lib.openid.ClientIdScheme
import at.asitplus.wallet.lib.openid.CredentialPresentationRequestBuilder
import at.asitplus.wallet.lib.openid.DCQLMatchingResult
import at.asitplus.wallet.lib.openid.OpenId4VpRequestOptions
import at.asitplus.wallet.lib.openid.OpenId4VpVerifier
import at.asitplus.wallet.lib.openid.VpTokenValidationResultDCQL
import data.credentials.FallbackCredentialAdapter
import data.credentials.labeledPresentationAttributes
import data.credentials.toGenericAttributeList
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * A stored credential whose scheme has no locally known type metadata must still be presentable:
 * the claims requested by the relying party have to end up in the authentication response.
 *
 * Mirrors the app's QR-code presentation flow: the persistent credential store,
 * [at.asitplus.wallet.app.common.PresentationService] (preparation + matching), the
 * route serialization of `AuthenticationViewRoute`, and `PresentationBuilderGraphView.onSubmit`
 * (submission mapping).
 */
class UnknownSchemeDcqlPresentationTest {

    @Test
    fun transmitsRequestedClaimsOfSdJwtCredentialWithUnknownScheme() = runTest {
        presentCredentialWithUnknownScheme(
            scheme = SdJwtFallbackCredentialScheme("https://example.com/credentials/unknown"),
            representation = SD_JWT,
        )
    }

    @Test
    fun transmitsRequestedClaimsOfIsoMdocCredentialWithUnknownScheme() = runTest {
        presentCredentialWithUnknownScheme(
            scheme = IsoMdocFallbackCredentialScheme("org.example.unknown.1"),
            representation = ISO_MDOC,
        )
    }

    /**
     * A query without `claims` requests no selectively disclosable claims: the wallet must only
     * return the claims that are mandatory to present (OpenID4VP 1.0, section 6.4.1).
     */
    @Test
    fun transmitsOnlyMandatoryClaimsOfSdJwtCredentialWithUnknownSchemeWhenNoneAreRequested() = runTest {
        presentCredentialWithUnknownScheme(
            scheme = SdJwtFallbackCredentialScheme("https://example.com/credentials/unknown"),
            representation = SD_JWT,
            requestedAttributes = null,
            expectedDisclosedClaims = emptySet(),
        )
    }

    private suspend fun presentCredentialWithUnknownScheme(
        scheme: CredentialScheme,
        representation: CredentialRepresentation,
        requestedAttributes: Set<String>? = setOf("family_name"),
        expectedDisclosedClaims: Set<String> = setOf("family_name"),
    ) {
        val keyMaterial = EphemeralKeyWithoutCert()
        val holderAgent = HolderAgent(
            keyMaterial = keyMaterial,
            subjectCredentialStore = PersistentSubjectCredentialStore(DummyDataStoreService(), Validator()),
        )
        holderAgent.storeCredential(
            IssuerAgent(
                keyMaterial = EphemeralKeyWithSelfSignedCert(),
                identifier = "https://issuer.example.com/".toUri(),
            ).issueCredential(
                credentialToBeIssued(scheme, representation, keyMaterial.publicKey)
            ).getOrThrow().toStoreCredentialInput()
        ).getOrThrow()

        val verifier = OpenId4VpVerifier(
            clientIdScheme = ClientIdScheme.PreRegistered("client-id", "http://rp.example.com/cb"),
        )
        val requestUrl = verifier.createAuthnRequest(
            OpenId4VpRequestOptions(
                presentationRequest = CredentialPresentationRequestBuilder(
                    credentials = setOf(
                        RequestOptionsCredential(
                            credentialScheme = scheme,
                            representation = representation,
                            attributePaths = requestedAttributes?.map { DCQLClaimsPathPointer(it) }?.toSet(),
                        )
                    ),
                ).toDCQLRequest(),
                responseMode = ResponseMode.DirectPost,
                responseUrl = RESPONSE_URL,
            ),
            OpenId4VpVerifier.CreationOptions.Query("http://wallet.example.com/"),
        ).getOrThrow().url

        var responseValidation: KmmResult<AuthnResponseResult>? = null
        val wallet = OpenId4VpWallet(
            engine = MockEngine { request ->
                if (request.url.toString().startsWith(RESPONSE_URL)) {
                    responseValidation = verifier.validateAuthnResponse(request.body.toByteArray().decodeToString())
                    respondOk()
                } else respondError(HttpStatusCode.NotFound)
            },
            keyMaterial = keyMaterial,
            holderAgent = holderAgent,
        )

        // the app serializes the preparation state into the navigation route (AuthenticationViewRoute)
        val preparationState = joseCompliantSerializer.decodeFromString<AuthorizationResponsePreparationState>(
            joseCompliantSerializer.encodeToString(
                wallet.startAuthorizationResponsePreparation(requestUrl).getOrThrow()
            )
        )
        val matching = assertIs<DCQLMatchingResult<SubjectCredentialStore.StoreEntry>>(
            wallet.getMatchingCredentials(preparationState).getOrThrow()
        )

        // the selection cards must at least list the credential's claims, like the details view does
        val storedCredential = matching.matchingResult.credentials.single()
        val genericAttributes = storedCredential.toGenericAttributeList()
        val labels = FallbackCredentialAdapter(genericAttributes, storedCredential, scheme)
            .labeledPresentationAttributes(genericAttributes)
            .map { it.first }
        assertTrue(
            labels.any { it.endsWith("family_name") } && labels.any { it.endsWith("given_name") },
            "selection card should label all claims of a credential with unknown scheme, got: $labels"
        )

        // as PresentationBuilderGraphView.onSubmit does, with every matching credential selected
        val submissions = matching.matchingResult.dcqlQueryMatchingResult.credentialMatchingResults
            .mapValues { (_, matches) ->
                matches.mapIndexedNotNull { index, match ->
                    match.getOrNull()?.let {
                        DCQLCredentialSubmissionOption(
                            credential = matching.matchingResult.credentials[index],
                            matchingResult = it,
                        )
                    }
                }
            }
        assertTrue(
            submissions.isNotEmpty() && submissions.values.all { it.isNotEmpty() },
            "credential with unknown scheme should match the DCQL query"
        )

        wallet.finalizeAuthorizationResponse(
            preparationState,
            CredentialPresentation.DCQLPresentation(
                presentationRequest = matching.presentationRequest,
                credentialQuerySubmissions = submissions,
            ),
        ).getOrThrow()

        val validated = assertNotNull(responseValidation, "relying party should have received a response").getOrThrow()
        val presentationResult = assertIs<VpTokenValidationResultDCQL>(
            assertNotNull(validated.vpTokenValidationResult).getOrThrow()
        ).credentialQueryResponseValidations.values.single().single().getOrThrow()
        val disclosedClaims = when (presentationResult) {
            is Verifier.VerifyPresentationResult.SuccessSdJwt -> presentationResult.disclosures.map { it.claimName }
            is Verifier.VerifyPresentationResult.SuccessIso -> presentationResult.documents
                .flatMap { it.validItems }.map { it.elementIdentifier }

            else -> emptyList()
        }
        assertEquals(expectedDisclosedClaims, disclosedClaims.filterNotNull().toSet())
    }

    private fun credentialToBeIssued(
        scheme: CredentialScheme,
        representation: CredentialRepresentation,
        subjectPublicKey: at.asitplus.signum.indispensable.CryptoPublicKey,
    ): CredentialToBeIssued = when (representation) {
        SD_JWT -> CredentialToBeIssued.VcSd(
            claims = listOf(
                ClaimToBeIssued("family_name", "Musterfrau"),
                ClaimToBeIssued("given_name", "Erika"),
            ),
            expiration = Clock.System.now() + 10.minutes,
            scheme = scheme as at.asitplus.wallet.lib.data.SdJwtCredentialScheme,
            subjectPublicKey = subjectPublicKey,
            userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
        )

        ISO_MDOC -> CredentialToBeIssued.Iso(
            issuerSignedItems = listOf(
                IssuerSignedItem(0U, Random.nextBytes(16), "family_name", "Musterfrau"),
                IssuerSignedItem(1U, Random.nextBytes(16), "given_name", "Erika"),
            ),
            expiration = Clock.System.now() + 10.minutes,
            scheme = scheme as at.asitplus.wallet.lib.data.IsoMdocCredentialScheme,
            subjectPublicKey = subjectPublicKey,
            userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
        )

        else -> throw IllegalArgumentException("representation")
    }

    companion object {
        private const val RESPONSE_URL = "http://rp.example.com/response"
    }
}

package ui.presentation

import at.asitplus.iso.IssuerSignedItem
import at.asitplus.iso.SessionTranscript
import at.asitplus.openid.OidcUserInfo
import at.asitplus.openid.OidcUserInfoExtended
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialQuerySelection
import at.asitplus.wallet.lib.agent.CreatePresentationResult
import at.asitplus.wallet.lib.agent.CredentialToBeIssued
import at.asitplus.wallet.lib.agent.DCQLMatchingResult
import at.asitplus.wallet.lib.agent.EphemeralKeyWithSelfSignedCert
import at.asitplus.wallet.lib.agent.EphemeralKeyWithoutCert
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.agent.IssuerAgent
import at.asitplus.wallet.lib.agent.PresentationRequestParameters
import at.asitplus.wallet.lib.agent.PresentationResponseParameters
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.agent.toStoreCredentialInput
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.IsoMdocFallbackCredentialScheme
import at.asitplus.wallet.lib.data.rfc3986.toUri
import data.storage.DummyDataStoreService
import data.storage.PersistentSubjectCredentialStore
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes

/**
 * Presents a PID selected through the DC API on Android for the request of eid.egiz.gv.at, which asks for one PID with
 * four alternative credential queries. Two of them match the stored ISO mdoc PID and request different claims, so the
 * claims disclosed depend on the query the system selected the PID for, as the picker showed the claims of that query.
 */
class FixedDcApiPidPresentationTest {

    @Test
    fun presentsPidOnceWithClaimsOfQueryWithoutAdministrativeNumber() = runTest {
        assertDisclosedClaims(
            selectedQuery = QUERY_WITHOUT_ADMINISTRATIVE_NUMBER,
            expectedClaims = setOf(
                "given_name", "family_name", "birth_date", "nationality", "place_of_birth", "issuing_country",
            ),
        )
    }

    @Test
    fun presentsPidOnceWithClaimsOfQueryWithAdministrativeNumber() = runTest {
        assertDisclosedClaims(
            selectedQuery = QUERY_WITH_ADMINISTRATIVE_NUMBER,
            expectedClaims = setOf(
                "given_name", "family_name", "birth_date", "nationality", "place_of_birth", "issuing_country",
                "personal_administrative_number",
            ),
        )
    }

    private suspend fun assertDisclosedClaims(selectedQuery: String, expectedClaims: Set<String>) {
        val keyMaterial = EphemeralKeyWithoutCert()
        val holder = HolderAgent(
            keyMaterial = keyMaterial,
            subjectCredentialStore = PersistentSubjectCredentialStore(DummyDataStoreService()),
        )
        holder.storeCredential(
            IssuerAgent(
                keyMaterial = EphemeralKeyWithSelfSignedCert(),
                identifier = "https://issuer.example.com/".toUri(),
            ).issueCredential(pid(keyMaterial)).getOrThrow().toStoreCredentialInput()
        ).getOrThrow()

        val presentationRequest = CredentialPresentationRequest.DCQLRequest(
            Json.decodeFromString<DCQLQuery>(EGIZ_DCQL_QUERY)
        )
        val selectedPid = holder.getCredentials().orEmpty().single()
        // As main.android.kt reads it from the entry the user selected in the system picker
        val selection = DCAPICredentialQuerySelection(selectedPid.getDcApiId(), selectedQuery)
        val matching = assertIs<DCQLMatchingResult<SubjectCredentialStore.StoreEntry>>(
            holder.matchPresentationRequestAgainstCredentialStore(
                presentationRequest,
                filterByIds = listOf(selectedPid.getDcApiId()),
            ).getOrThrow()
        )
        // Both ISO mdoc queries match the PID, so our matching alone can not tell which one the user agreed to
        assertEquals(
            setOf(QUERY_WITH_ADMINISTRATIVE_NUMBER, QUERY_WITHOUT_ADMINISTRATIVE_NUMBER),
            matching.matchingResult.credentialQueryMatches.filterValues { it.isNotEmpty() }.keys.map { it.string }
                .toSet(),
        )

        val submissions = matching.toFixedDcApiSubmissions(listOf(selection)) { it.getDcApiId() }.getOrThrow()
        assertEquals(setOf(DCQLCredentialQueryIdentifier(selectedQuery)), submissions.keys)

        val response = holder.createPresentation(
            request = PresentationRequestParameters(
                nonce = "nonce",
                audience = "x509_hash:verifier",
                calcIsoSessionTranscript = { SessionTranscript.Qr(Random.nextBytes(32), Random.nextBytes(32)) },
            ),
            credentialPresentation = CredentialPresentation.DCQLPresentation(
                presentationRequest = presentationRequest,
                credentialQuerySubmissions = submissions,
            ),
        ).getOrThrow()

        val presentations = assertIs<PresentationResponseParameters.DCQLParameters>(response).verifiablePresentations
        assertEquals(setOf(DCQLCredentialQueryIdentifier(selectedQuery)), presentations.keys)
        val document = assertIs<CreatePresentationResult.DeviceResponse>(presentations.values.single().single())
            .deviceResponse.documents.orEmpty().single()
        val disclosedClaims = document.issuerSigned.namespaces.orEmpty().getValue(PID_DOC_TYPE)
            .entries.map { it.value.elementIdentifier }.toSet()
        assertEquals(expectedClaims, disclosedClaims)
    }

    private fun pid(keyMaterial: EphemeralKeyWithoutCert) = CredentialToBeIssued.Iso(
        issuerSignedItems = listOf(
            "given_name" to "Erika",
            "family_name" to "Musterfrau",
            "birth_date" to "1964-08-12",
            "nationality" to "AT",
            "place_of_birth" to "Wien",
            "issuing_country" to "AT",
            "personal_administrative_number" to "123456789",
            "resident_city" to "Wien",
        ).mapIndexed { index, (name, value) ->
            IssuerSignedItem(index.toUInt(), Random.nextBytes(16), name, value)
        },
        expiration = Clock.System.now() + 10.minutes,
        scheme = IsoMdocFallbackCredentialScheme(PID_DOC_TYPE),
        subjectPublicKey = keyMaterial.publicKey,
        userInfo = OidcUserInfoExtended.fromOidcUserInfo(OidcUserInfo("subject")).getOrThrow(),
    )

    companion object {
        private const val PID_DOC_TYPE = "eu.europa.ec.eudi.pid.1"
        private const val QUERY_WITH_ADMINISTRATIVE_NUMBER = "7bc6c5f5-430a-4908-8b77-224c69eb98cd"
        private const val QUERY_WITHOUT_ADMINISTRATIVE_NUMBER = "62aa28b0-096e-49e6-87c1-e7bd0016d219"

        /** The `dcql_query` of the request of eid.egiz.gv.at */
        private val EGIZ_DCQL_QUERY = """
            {
              "credentials": [
                {
                  "id": "730afb2b-1381-4153-a84e-fd1185f5b8a1",
                  "meta": { "vct_values": ["urn:eudi:pid:1"] },
                  "claims": [
                    { "path": ["given_name"] }, { "path": ["family_name"] }, { "path": ["birthdate"] },
                    { "path": ["nationalities"] }, { "path": ["place_of_birth"] }, { "path": ["issuing_country"] }
                  ],
                  "format": "dc+sd-jwt"
                },
                {
                  "id": "5233797d-cf24-456d-9624-c9c93cf0f277",
                  "meta": { "vct_values": ["urn:eudi:pid:1"] },
                  "claims": [
                    { "path": ["place_of_birth"] }, { "path": ["birthdate"] }, { "path": ["issuing_country"] },
                    { "path": ["personal_administrative_number"] }, { "path": ["nationalities"] },
                    { "path": ["given_name"] }, { "path": ["family_name"] }
                  ],
                  "format": "dc+sd-jwt"
                },
                {
                  "id": "7bc6c5f5-430a-4908-8b77-224c69eb98cd",
                  "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" },
                  "claims": [
                    { "path": ["eu.europa.ec.eudi.pid.1", "place_of_birth"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "nationality"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "issuing_country"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "birth_date"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "personal_administrative_number"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "given_name"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "family_name"] }
                  ],
                  "format": "mso_mdoc"
                },
                {
                  "id": "62aa28b0-096e-49e6-87c1-e7bd0016d219",
                  "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" },
                  "claims": [
                    { "path": ["eu.europa.ec.eudi.pid.1", "given_name"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "family_name"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "birth_date"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "nationality"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "place_of_birth"] },
                    { "path": ["eu.europa.ec.eudi.pid.1", "issuing_country"] }
                  ],
                  "format": "mso_mdoc"
                }
              ],
              "credential_sets": [
                {
                  "options": [
                    ["730afb2b-1381-4153-a84e-fd1185f5b8a1"],
                    ["5233797d-cf24-456d-9624-c9c93cf0f277"],
                    ["7bc6c5f5-430a-4908-8b77-224c69eb98cd"],
                    ["62aa28b0-096e-49e6-87c1-e7bd0016d219"]
                  ]
                }
              ]
            }
        """.trimIndent()
    }
}

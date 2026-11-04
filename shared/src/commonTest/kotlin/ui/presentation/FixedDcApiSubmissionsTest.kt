package ui.presentation

import at.asitplus.KmmResult
import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLQuery
import at.asitplus.openid.dcql.DCQLQueryMatchingResult
import at.asitplus.wallet.app.common.dcapi.DCAPICredentialQuerySelection
import at.asitplus.wallet.lib.agent.DCQLMatchingResult
import at.asitplus.wallet.lib.agent.HolderDCQLQueryMatchingResult
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The credentials selected by the system through the DC API may match several credential queries, e.g. the
 * alternative options of one credential set, but must be presented once only, for the query the system selected.
 */
class FixedDcApiSubmissionsTest {

    // As requested by eid.egiz.gv.at: one PID as SD-JWT or ISO mdoc, each with two alternative queries
    private val alternativePidQueries = Json.decodeFromString<DCQLQuery>(
        """
        {
          "credentials": [
            { "id": "sd-jwt-1", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
            { "id": "sd-jwt-2", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
            { "id": "mdoc-1", "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } },
            { "id": "mdoc-2", "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } }
          ],
          "credential_sets": [
            { "options": [ ["sd-jwt-1"], ["sd-jwt-2"], ["mdoc-1"], ["mdoc-2"] ] }
          ]
        }
        """.trimIndent()
    )

    @Test
    fun submitsCredentialForSelectedQueryOnly() {
        val submissions = alternativePidQueries.matching(
            credentials = listOf("mdoc", "other mdoc"),
            matchingQueries = setOf("mdoc-1", "mdoc-2"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("mdoc", "mdoc-2"))) { it }.getOrThrow()

        assertEquals(setOf(DCQLCredentialQueryIdentifier("mdoc-2")), submissions.keys)
        assertEquals(listOf("mdoc"), submissions.values.single().map { it.credential })
    }

    @Test
    fun failsForSelectedQueryNotSatisfiedByCredential() {
        val result = alternativePidQueries.matching(
            credentials = listOf("mdoc"),
            matchingQueries = setOf("mdoc-1"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("mdoc", "mdoc-2"))) { it }

        assertTrue(result.isFailure)
    }

    @Test
    fun failsForSelectedUnknownQuery() {
        val result = alternativePidQueries.matching(
            credentials = listOf("mdoc"),
            matchingQueries = setOf("mdoc-1"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("mdoc", "unknown"))) { it }

        assertTrue(result.isFailure)
    }

    @Test
    fun failsForSelectedUnavailableCredential() {
        val result = alternativePidQueries.matching(
            credentials = listOf("mdoc"),
            matchingQueries = setOf("mdoc-1"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("other mdoc", "mdoc-1"))) { it }

        assertTrue(result.isFailure)
    }

    @Test
    fun submitsSdJwtForSelectedOneOfAlternativeQueries() {
        val dcqlQuery = Json.decodeFromString<DCQLQuery>(
            """
            {
              "credentials": [
                { "id": "sd-jwt-1", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
                { "id": "sd-jwt-2", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } }
              ],
              "credential_sets": [
                { "options": [ ["sd-jwt-1"], ["sd-jwt-2"] ] }
              ]
            }
            """.trimIndent()
        )

        // The matcher lists the PID once per query, the user selected the entry of the second one
        val submissions = dcqlQuery.matching(
            credentials = listOf("sd-jwt"),
            matchingQueries = setOf("sd-jwt-1", "sd-jwt-2"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("sd-jwt", "sd-jwt-2"))) { it }.getOrThrow()

        assertEquals(setOf(DCQLCredentialQueryIdentifier("sd-jwt-2")), submissions.keys)
        assertEquals(listOf("sd-jwt"), submissions.values.single().map { it.credential })
    }

    @Test
    fun submitsSdJwtForEachOfRequiredQueries() {
        // Without credential sets, both queries are required, and the matcher offers a set with one slot per query
        val submissions = requiredSdJwtQueries.matching(
            credentials = listOf("sd-jwt"),
            matchingQueries = setOf("sd-jwt-1", "sd-jwt-2"),
        ).toFixedDcApiSubmissions(
            listOf(
                DCAPICredentialQuerySelection("sd-jwt", "sd-jwt-1"),
                DCAPICredentialQuerySelection("sd-jwt", "sd-jwt-2"),
            )
        ) { it }.getOrThrow()

        assertEquals(setOf("sd-jwt-1", "sd-jwt-2").map { DCQLCredentialQueryIdentifier(it) }.toSet(), submissions.keys)
        submissions.values.forEach { assertEquals(listOf("sd-jwt"), it.map { it.credential }) }
    }

    @Test
    fun failsForSelectionMissingRequiredSdJwtQuery() {
        // A Credential Manager runtime without sets lets the user select a single entry only
        val result = requiredSdJwtQueries.matching(
            credentials = listOf("sd-jwt"),
            matchingQueries = setOf("sd-jwt-1", "sd-jwt-2"),
        ).toFixedDcApiSubmissions(listOf(DCAPICredentialQuerySelection("sd-jwt", "sd-jwt-1"))) { it }

        assertTrue(result.isFailure)
    }

    private val requiredSdJwtQueries = Json.decodeFromString<DCQLQuery>(
        """
        {
          "credentials": [
            { "id": "sd-jwt-1", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } },
            { "id": "sd-jwt-2", "format": "dc+sd-jwt", "meta": { "vct_values": ["urn:eudi:pid:1"] } }
          ]
        }
        """.trimIndent()
    )

    @Test
    fun submitsOnlyFirstSatisfiableOptionOfCredentialSetWithoutSelectedQueries() {
        val dcqlQuery = alternativePidQueries
        val submissions = dcqlQuery.matching(
            credentials = listOf("selected mdoc"),
            matchingQueries = setOf("mdoc-1", "mdoc-2"),
        ).toFixedDcApiSubmissions(emptyList()) { it }.getOrThrow()

        assertEquals(setOf(DCQLCredentialQueryIdentifier("mdoc-1")), submissions.keys)
        assertEquals(listOf("selected mdoc"), submissions.values.single().map { it.credential })
    }

    @Test
    fun submitsEveryQueryWithoutCredentialSets() {
        val dcqlQuery = Json.decodeFromString<DCQLQuery>(
            """
            {
              "credentials": [
                { "id": "pid", "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } },
                { "id": "mdl", "format": "mso_mdoc", "meta": { "doctype_value": "org.iso.18013.5.1.mDL" } }
              ]
            }
            """.trimIndent()
        )

        val submissions = dcqlQuery.matching(
            credentials = listOf("selected credential"),
            matchingQueries = setOf("pid", "mdl"),
        ).toFixedDcApiSubmissions(emptyList()) { it }.getOrThrow()

        assertEquals(setOf("pid", "mdl").map { DCQLCredentialQueryIdentifier(it) }.toSet(), submissions.keys)
    }

    @Test
    fun failsWithoutMatchForRequiredQuery() {
        val dcqlQuery = Json.decodeFromString<DCQLQuery>(
            """
            {
              "credentials": [
                { "id": "pid", "format": "mso_mdoc", "meta": { "doctype_value": "eu.europa.ec.eudi.pid.1" } },
                { "id": "mdl", "format": "mso_mdoc", "meta": { "doctype_value": "org.iso.18013.5.1.mDL" } }
              ]
            }
            """.trimIndent()
        )

        val result = dcqlQuery.matching(
            credentials = listOf("selected pid"),
            matchingQueries = setOf("pid"),
        ).toFixedDcApiSubmissions(emptyList()) { it }

        assertTrue(result.isFailure)
    }

    /** Matches every credential to each query of [matchingQueries], and to no other query. */
    private fun DCQLQuery.matching(
        credentials: List<String>,
        matchingQueries: Set<String>,
    ) = DCQLMatchingResult(
        presentationRequest = CredentialPresentationRequest.DCQLRequest(this),
        matchingResult = HolderDCQLQueryMatchingResult(
            dcqlQueryMatchingResult = DCQLQueryMatchingResult(
                this.credentials.associate { query ->
                    query.id to credentials.map {
                        if (query.id.string in matchingQueries) {
                            KmmResult.success(DCQLCredentialQueryMatchingResult.AllClaimsMatchingResult)
                        } else {
                            KmmResult.failure(IllegalArgumentException("No match"))
                        }
                    }
                }
            ),
            credentials = credentials,
        ),
    )
}

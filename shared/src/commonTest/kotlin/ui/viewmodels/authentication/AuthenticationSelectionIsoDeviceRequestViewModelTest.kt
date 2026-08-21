package ui.viewmodels.authentication

import at.asitplus.iso.DeviceRequest
import at.asitplus.wallet.lib.agent.HolderIsoDeviceRetrievalQueryMatchingResult
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalClaimMatch
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalCredentialMatch
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalQueryMatchingResult
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import io.github.z4kn4fein.semver.Version
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AuthenticationSelectionIsoDeviceRequestViewModelTest {

    @Test
    fun preservesOrderedRepeatedRequestsAndOneSelectionPerRequest() {
        val matching = matchingResult(
            credentials = listOf("first", "second"),
            matches = listOf(
                listOf(match(0, "ns", "given_name"), match(1, "ns", "given_name")),
                listOf(match(1, "ns", "family_name"), match(0, "ns", "family_name")),
            ),
        )
        var submitted: CredentialPresentationSubmissions<String>? = null
        val viewModel = AuthenticationSelectionIsoDeviceRequestViewModel(
            credentialMatchingResult = matching,
            confirmSelections = { submitted = it },
            navigateUp = {},
        )
        viewModel.credentialSelection.getValue(0).value = viewModel.requests[0][1]

        viewModel.onNext()
        viewModel.onNext()

        val disclosures = (submitted as IsoDeviceRequestCredentialSubmissions<*>).submissions.toList()
        assertEquals(listOf(0, 1), disclosures.map { it.docRequestIndex })
        assertEquals(listOf("second", "second"), disclosures.map { it.credential })
        assertEquals(
            listOf("$['ns']['given_name']", "$['ns']['family_name']"),
            disclosures.map { it.disclosedAttributes.single().toString() },
        )
    }

    @Test
    fun rejectsAnyDocumentRequestWithoutACompleteMatch() {
        val matching = matchingResult(
            credentials = listOf("credential"),
            matches = listOf(listOf(match(0, "ns", "given_name")), emptyList()),
        )

        assertFailsWith<IllegalStateException> {
            AuthenticationSelectionIsoDeviceRequestViewModel(matching, {}, {})
        }
    }

    private fun matchingResult(
        credentials: List<String>,
        matches: List<List<IsoDeviceRetrievalCredentialMatch>>,
    ) = IsoDeviceRetrievalMatchingResult(
        presentationRequest = CredentialPresentationRequest.IsoDeviceRetrieval(
            DeviceRequest(parsedVersion = Version(1, 0), docRequests = emptyArray())
        ),
        matchingResult = HolderIsoDeviceRetrievalQueryMatchingResult(
            credentials = credentials,
            queryMatchingResult = IsoDeviceRetrievalQueryMatchingResult(matches),
        ),
    )

    private fun match(credentialIndex: Int, namespace: String, claim: String) =
        IsoDeviceRetrievalCredentialMatch(
            credentialIndex = credentialIndex,
            requestedClaims = listOf(IsoDeviceRetrievalClaimMatch(namespace, claim, "value")),
        )
}

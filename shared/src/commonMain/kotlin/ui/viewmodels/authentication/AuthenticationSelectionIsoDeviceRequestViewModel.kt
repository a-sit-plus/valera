package ui.viewmodels.authentication

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateMap
import at.asitplus.wallet.lib.agent.DeviceRequestCredentialDisclosure
import at.asitplus.wallet.lib.agent.IsoDeviceRetrievalMatchingResult

class AuthenticationSelectionIsoDeviceRequestViewModel<Credential : Any>(
    val credentialMatchingResult: IsoDeviceRetrievalMatchingResult<Credential>,
    val confirmSelections: (CredentialPresentationSubmissions<Credential>) -> Unit,
    val navigateUp: () -> Unit,
) {
    val requests: List<List<DeviceRequestCredentialDisclosure<Credential>>> =
        credentialMatchingResult.matchingResult.documentMatches

    val requestIterator = mutableStateOf(0)
    val credentialSelection: SnapshotStateMap<Int, MutableState<DeviceRequestCredentialDisclosure<Credential>>> =
        mutableStateMapOf()

    init {
        requests.forEachIndexed { requestIndex, matches ->
            credentialSelection[requestIndex] = mutableStateOf(
                matches.firstOrNull()
                    ?: throw IllegalStateException("ISO document request at index $requestIndex has no complete match")
            )
        }
    }

    val onBack = {
        if (requestIterator.value > 0) requestIterator.value -= 1 else navigateUp()
    }

    val onNext = {
        if (requestIterator.value < requests.lastIndex) {
            requestIterator.value += 1
        } else {
            val submissions = requests.indices.map { requestIndex ->
                requireNotNull(credentialSelection[requestIndex]?.value) {
                    "No credential selected for ISO document request at index $requestIndex"
                }
            }
            confirmSelections(IsoDeviceRequestCredentialSubmissions(submissions))
        }
    }
}

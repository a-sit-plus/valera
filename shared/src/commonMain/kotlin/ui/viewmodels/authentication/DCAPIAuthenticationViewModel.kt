package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.app.common.dcapi.DCAPIRequest
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.CredentialAdapter
import kotlinx.coroutines.runBlocking

class DCAPIAuthenticationViewModel(
    spImage: ImageBitmap? = null,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    val dcApiRequest: DCAPIRequest,
    onClickLogo: () -> Unit
) : AuthenticationViewModel(
    spName = dcApiRequest.callingPackageName,
    spLocation = dcApiRequest.callingOrigin ?: dcApiRequest.callingPackageName!!,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain,
    onClickLogo
) {
    override val descriptors = dcApiRequest.requestedData.mapNotNull {
        DifInputDescriptor(id = MobileDrivingLicenceScheme.isoDocType, constraints = Constraint(fields = it.value.map { requestedAttribute -> ConstraintField(path = listOf(
            NormalizedJsonPath(
                NormalizedJsonPathSegment.NameSegment(it.key),
                NormalizedJsonPathSegment.NameSegment(requestedAttribute.first),
            ).toString()), intentToRetain = requestedAttribute.second) }))
    }

    override val transactionData = null

    override fun findMatchingCredentials(): Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>> {
        return runBlocking {
                walletMain.holderAgent.matchInputDescriptorsAgainstCredentialStore(
                    inputDescriptors = descriptors,
                    fallbackFormatHolder = null,
                ).getOrThrow()
            }.map { (key, value) -> key to value.filter { (cred, _) -> CredentialAdapter.getId(cred).hashCode() == dcApiRequest.credentialId } }.toMap()
    }

    override suspend fun finalizationMethod(submission: Map<String, CredentialSubmission>) =
        walletMain.presentationService.finalizeDCAPIPreviewPresentation(submission, dcApiRequest)

}
package ui.viewmodels.Authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.misc.getRequestOptionParameters
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme
import data.credentials.CredentialAdapter
import data.dcapi.DCAPIRequest
import kotlinx.coroutines.runBlocking


class DCAPIAuthenticationViewModel(
    spImage: ImageBitmap? = null,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    val dcApiRequest: DCAPIRequest
) : AuthenticationViewModel(
    spName = dcApiRequest.callingPackageName,
    spLocation = dcApiRequest.callingOrigin ?: dcApiRequest.callingPackageName!!,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain
) {
    override val descriptors = dcApiRequest.requestedData.mapNotNull {
        DifInputDescriptor(id = MobileDrivingLicenceScheme.isoDocType, constraints = Constraint(fields = it.value.map { requestedAttribute -> ConstraintField(path = listOf(
            NormalizedJsonPath(
                NormalizedJsonPathSegment.NameSegment(it.key),
                NormalizedJsonPathSegment.NameSegment(requestedAttribute.first),
            ).toString()), intentToRetain = requestedAttribute.second) }))
    }

    override val parametersMap = descriptors.mapNotNull {
        val parameter = it.getRequestOptionParameters() ?: return@mapNotNull null
        it.id to parameter
    }.toMap()

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
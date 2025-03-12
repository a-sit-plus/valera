package ui.viewmodels.Authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NodeList
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.CredentialSubmission
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.iso.DeviceResponse
import com.android.identity.request.MdocRequest
import kotlinx.coroutines.runBlocking

class PresentationViewModel(
    val presentationStateModel: PresentationStateModel,
    navigateUp: () -> Unit,
    onAuthenticationSuccess: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    spImage: ImageBitmap? = null
) : AuthenticationViewModel(
    spName = null,
    spLocation = "Local Presentation",
    spImage,
    navigateUp,
    onAuthenticationSuccess,
    navigateToHomeScreen,
    walletMain
) {
    fun initWithMdocRequest(
        parsedRequest: List<MdocRequest>,
        finishFunction: (DeviceResponse) -> Unit
    ) {
        descriptors.addAll(parsedRequest.map {
            DifInputDescriptor(
                id = it.docType,
                constraints = Constraint(fields = it.claims.map { requestedAttribute ->
                    ConstraintField(
                        path = listOf(
                            NormalizedJsonPath(
                                NormalizedJsonPathSegment.NameSegment(requestedAttribute.namespaceName),
                                NormalizedJsonPathSegment.NameSegment(requestedAttribute.dataElementName),
                            ).toString()
                        ), intentToRetain = requestedAttribute.intentToRetain
                    )
                })
            )
        })
        this.finishFunction = finishFunction
    }

    private var finishFunction: ((DeviceResponse) -> Unit)? = null

    override val descriptors = mutableListOf<InputDescriptor>()
    override val transactionData: TransactionData? = null

    /*override val parametersMap = descriptors.mapNotNull {
        val parameter = it.getRequestOptionParameters().getOrElse { return@mapNotNull null }
        it.id to parameter
    }.toMap()*/

    override fun findMatchingCredentials(): Map<String, Map<SubjectCredentialStore.StoreEntry, Map<ConstraintField, NodeList>>> {
        return runBlocking {
            walletMain.holderAgent.matchInputDescriptorsAgainstCredentialStore(
                inputDescriptors = descriptors,
                fallbackFormatHolder = null,
            ).getOrThrow()
        }
    }

    override suspend fun finalizationMethod(submission: Map<String, CredentialSubmission>) =
        finishFunction?.let {
            walletMain.presentationService.finalizeLocalPresentation(submission, it)
        } ?: throw IllegalStateException("No finish method found")


}
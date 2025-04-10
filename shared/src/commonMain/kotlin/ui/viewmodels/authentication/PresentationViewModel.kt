package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.KmmResult
import at.asitplus.catching
import at.asitplus.dif.Constraint
import at.asitplus.dif.ConstraintField
import at.asitplus.dif.DifInputDescriptor
import at.asitplus.dif.FormatContainerJwt
import at.asitplus.dif.FormatHolder
import at.asitplus.dif.PresentationDefinition
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.iso.DeviceRequest
import at.asitplus.wallet.lib.iso.SessionTranscript

class PresentationViewModel(
    val presentationStateModel: PresentationStateModel,
    navigateUp: () -> Unit,
    onAuthenticationSuccess: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    spImage: ImageBitmap? = null,
    onClickLogo: () -> Unit,
) : AuthenticationViewModel(
    spName = null,
    spLocation = "Local Presentation",
    spImage,
    navigateUp,
    onAuthenticationSuccess,
    navigateToHomeScreen,
    walletMain,
    onClickLogo
) {
    private var descriptors: List<DifInputDescriptor> = listOf()
    private var finishFunction: ((ByteArray) -> Unit)? = null
    private var sessionTranscript: SessionTranscript? = null

    fun initWithDeviceRequest(
        parsedRequest: DeviceRequest,
        finishFunction: (ByteArray) -> Unit,
        sessionTranscript: SessionTranscript?
    ) {
        descriptors = parsedRequest.docRequests.map {
            val itemsRequest = it.itemsRequest.value
            DifInputDescriptor(
                id = itemsRequest.docType,
                format = FormatHolder(msoMdoc = FormatContainerJwt()),
                constraints = Constraint(fields = itemsRequest.namespaces.flatMap { requestedNamespace ->
                    requestedNamespace.value.entries.map { requestedAttrribute ->
                        ConstraintField(
                            path = listOf(
                                NormalizedJsonPath(
                                    NormalizedJsonPathSegment.NameSegment(requestedNamespace.key),
                                    NormalizedJsonPathSegment.NameSegment(requestedAttrribute.key),
                                ).toString()
                            ), intentToRetain = requestedAttrribute.value
                        )
                    }
                })
            )
        }
        this.finishFunction = finishFunction
        this.sessionTranscript = sessionTranscript
    }

    override val transactionData: at.asitplus.openid.TransactionData? = null

    override val presentationRequest: CredentialPresentationRequest.PresentationExchangeRequest
        get() = CredentialPresentationRequest.PresentationExchangeRequest(
            presentationDefinition = PresentationDefinition(
                inputDescriptors = descriptors
            )
        )

    override suspend fun findMatchingCredentials(): KmmResult<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        catching {
            PresentationExchangeMatchingResult(
                presentationRequest = CredentialPresentationRequest.PresentationExchangeRequest(
                    presentationDefinition = PresentationDefinition(
                        inputDescriptors = presentationRequest.presentationDefinition.inputDescriptors,
                    )
                ),
                matchingInputDescriptorCredentials = walletMain.holderAgent.matchInputDescriptorsAgainstCredentialStore(
                    inputDescriptors = presentationRequest.presentationDefinition.inputDescriptors,
                    fallbackFormatHolder = null,
                ).getOrThrow()
            )
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) =
        finishFunction?.let {
            walletMain.presentationService.finalizeLocalPresentation(
                credentialPresentation = when (credentialPresentation) {
                    is CredentialPresentation.PresentationExchangePresentation -> credentialPresentation
                    else -> throw IllegalArgumentException()
                },
                it,
                spName,
                sessionTranscript!!
            )
        } ?: throw IllegalStateException("No finish method found")

}
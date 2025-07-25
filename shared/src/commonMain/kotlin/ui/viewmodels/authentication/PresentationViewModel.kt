package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catchingUnwrapped
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
import at.asitplus.iso.DeviceRequest
import at.asitplus.iso.SessionTranscript
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult

class PresentationViewModel(
    val presentationStateModel: PresentationStateModel,
    navigateUp: () -> Unit,
    onAuthenticationSuccess: (redirectUrl: String?) -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    spImage: ImageBitmap? = null,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit
) : AuthenticationViewModel(
    spName = null,
    spLocation = "Local Presentation",
    spImage,
    navigateUp,
    onAuthenticationSuccess,
    navigateToHomeScreen,
    walletMain,
    onClickLogo,
    onClickSettings
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
                    requestedNamespace.value.entries.map { requestedAttribute ->
                        ConstraintField(
                            path = listOf(
                                NormalizedJsonPath(
                                    NormalizedJsonPathSegment.NameSegment(requestedNamespace.key),
                                    NormalizedJsonPathSegment.NameSegment(requestedAttribute.key),
                                ).toString()
                            ), intentToRetain = requestedAttribute.value
                        )
                    }
                })
            )
        }
        this.finishFunction = finishFunction
        this.sessionTranscript = sessionTranscript
    }

    override val transactionData = null

    override val presentationRequest: CredentialPresentationRequest.PresentationExchangeRequest
        get() = CredentialPresentationRequest.PresentationExchangeRequest(
            presentationDefinition = PresentationDefinition(
                inputDescriptors = descriptors
            )
        )

    override suspend fun findMatchingCredentials(): Result<CredentialMatchingResult<SubjectCredentialStore.StoreEntry>> =
        catchingUnwrapped {
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
            OpenId4VpWallet.AuthenticationSuccess(null)
        } ?: throw IllegalStateException("No finish method found")

}

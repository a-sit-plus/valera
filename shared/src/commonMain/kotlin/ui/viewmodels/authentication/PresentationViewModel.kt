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
import at.asitplus.rqes.collection_entries.TransactionData
import at.asitplus.wallet.app.common.WalletMain
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialPresentation
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import org.multipaz.request.MdocRequest

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

    fun initWithMdocRequest(
        parsedRequest: List<MdocRequest>,
        finishFunction: (ByteArray) -> Unit
    ) {
        val requester: MutableList<String?> = mutableListOf()
        descriptors = parsedRequest.map {
            requester.add(it.requester.appId ?: it.requester.websiteOrigin)
            DifInputDescriptor(
                id = it.docType,
                format = FormatHolder(msoMdoc = FormatContainerJwt()),
                constraints = Constraint(fields = it.requestedClaims.map { requestedAttribute ->
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
        }
        this.finishFunction = finishFunction
        check(requester.all { it == requester.firstOrNull() })
        this.spName = requester.firstOrNull { it != null }
    }

    private var finishFunction: ((ByteArray) -> Unit)? = null

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
                spName
            )
        } ?: throw IllegalStateException("No finish method found")

}
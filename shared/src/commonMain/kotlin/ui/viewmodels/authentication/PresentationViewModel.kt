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
import at.asitplus.wallet.lib.iso.SessionTranscript
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
    private var finishFunction: ((ByteArray) -> Unit)? = null
    private var encodedSessionTranscript: ByteArray = ByteArray(0)
    private var sessionTranscript: SessionTranscript? = null

    fun initWithMdocRequest(
        parsedRequest: List<MdocRequest>,
        finishFunction: (ByteArray) -> Unit,
        encodedSessionTranscript: ByteArray,
        sessionTranscript: SessionTranscript?
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
        this.encodedSessionTranscript = encodedSessionTranscript
        this.finishFunction = finishFunction
        this.encodedSessionTranscript = encodedSessionTranscript
        check(requester.all { it == requester.firstOrNull() })
        this.spName = requester.firstOrNull { it != null }
        this.sessionTranscript = sessionTranscript
    }

    /*fun initWithAsitRequest(
        parsedRequest: Array<DocRequest>,
        finishFunction: (DeviceResponse) -> Unit
    ) {
        descriptors.addAll(parsedRequest.map {
            DifInputDescriptor(
                id = it.itemsRequest.value.docType,
                constraints = Constraint(fields = it.itemsRequest.value.namespaces.map { (namespace, itemsRequestList) ->
                    itemsRequestList.entries.map {
                    ConstraintField(
                        path = listOf(
                            NormalizedJsonPath(
                                NormalizedJsonPathSegment.NameSegment(namespace),
                                NormalizedJsonPathSegment.NameSegment(itemsRequestList.entries[0].requestedAttribute.dataElementName),
                            ).toString()
                        ), intentToRetain = requestedAttribute.intentToRetain
                    )
                }
                })

            )
        })
        this.finishFunction = finishFunction
    }*/

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
                encodedSessionTranscript,
                sessionTranscript!!
            )
        } ?: throw IllegalStateException("No finish method found")

}
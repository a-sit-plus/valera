package ui.viewmodels.authentication

import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.dcapi.request.PreviewDCAPIRequest
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
import at.asitplus.wallet.lib.openid.CredentialMatchingResult
import at.asitplus.wallet.lib.openid.PresentationExchangeMatchingResult
import at.asitplus.wallet.mdl.MobileDrivingLicenceScheme

class PreviewDCAPIAuthenticationViewModel(
    spImage: ImageBitmap? = null,
    navigateUp: () -> Unit,
    navigateToAuthenticationSuccessPage: (redirectUrl: String?) -> Unit,
    navigateToHomeScreen: () -> Unit,
    walletMain: WalletMain,
    val dcApiRequestPreview: PreviewDCAPIRequest,
    onClickLogo: () -> Unit,
    onClickSettings: () -> Unit,
) : AuthenticationViewModel(
    spName = dcApiRequestPreview.callingPackageName,
    spLocation = dcApiRequestPreview.callingOrigin ?: dcApiRequestPreview.callingPackageName!!,
    spImage,
    navigateUp,
    navigateToAuthenticationSuccessPage,
    navigateToHomeScreen,
    walletMain,
    onClickLogo,
    onClickSettings
) {
    override val presentationRequest: CredentialPresentationRequest.PresentationExchangeRequest
        get() = CredentialPresentationRequest.PresentationExchangeRequest(
            presentationDefinition = PresentationDefinition(
                inputDescriptors = dcApiRequestPreview.requestedData.mapNotNull {
                    DifInputDescriptor(
                        id = MobileDrivingLicenceScheme.isoDocType,
                        format = FormatHolder(msoMdoc = FormatContainerJwt()),
                        constraints = Constraint(
                            fields = it.value.map { requestedAttribute ->
                                ConstraintField(
                                    path = listOf(
                                        NormalizedJsonPath(
                                            NormalizedJsonPathSegment.NameSegment(it.key),
                                            NormalizedJsonPathSegment.NameSegment(requestedAttribute.first),
                                        ).toString()
                                    ),
                                    intentToRetain = requestedAttribute.second,
                                )
                            },
                        )
                    )
                }
            )
        )

    override val transactionData = null

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
                ).getOrThrow().map { (key, value) ->
                    key to value.filter { (cred, _) ->
                        catching { cred.getDcApiId() == dcApiRequestPreview.credentialId }.getOrElse { false }
                    }
                }.toMap()
            )
        }

    override suspend fun finalizationMethod(credentialPresentation: CredentialPresentation) =
        walletMain.presentationService.finalizeDCAPIPreviewPresentation(
            credentialPresentation = when (credentialPresentation) {
                is CredentialPresentation.PresentationExchangePresentation -> credentialPresentation
                else -> throw IllegalArgumentException()
            },
            dcApiRequestPreview,
        )
}

package ui.composables

import androidx.compose.runtime.Composable
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import org.jetbrains.compose.resources.stringResource

@Composable
fun PresentationRequestPreview(
    presentationRequest: CredentialPresentationRequest,
) {
    when(presentationRequest) {
        is CredentialPresentationRequest.DCQLRequest -> TODO()

        is CredentialPresentationRequest.PresentationExchangeRequest -> {
            presentationRequest.presentationDefinition.inputDescriptors.forEach { inputDescriptor ->
                catchingUnwrapped { inputDescriptor.extractConsentData() }.onSuccess { (representation, scheme, attributes) ->
                    val schemeName = scheme.uiLabel()
                    val format = representation.name
                    val list = attributes.mapNotNull { attribute ->
                        val resource = scheme.getLocalization(NormalizedJsonPath(attribute.key.segments.last())) ?: return@mapNotNull null
                        val text =
                            catchingUnwrapped { stringResource(resource) }.getOrElse { attribute.key.toString() }
                        text to attribute.value
                    }.toMap()
                    ConsentAttributesSection(
                        title = "$schemeName (${format})",
                        attributes = list
                    )
                }
            }
        }
    }
}
package ui.composables

import androidx.compose.runtime.Composable
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_invalid_dcql_query
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import org.jetbrains.compose.resources.stringResource

@Composable
fun PresentationRequestPreview(
    presentationRequest: CredentialPresentationRequest,
    onError: (Throwable) -> Unit,
) {
    when (presentationRequest) {
        is CredentialPresentationRequest.DCQLRequest -> DcqlRequestPreview(
            presentationRequest,
            onError = onError,
        )

        is CredentialPresentationRequest.PresentationExchangeRequest -> PresentationExchangeRequestPreview(
            presentationRequest,
            onError = onError,
        )
    }
}

@Composable
fun PresentationExchangeRequestPreview(
    presentationRequest: CredentialPresentationRequest.PresentationExchangeRequest,
    onError: (Throwable) -> Unit,
) {
    presentationRequest.presentationDefinition.inputDescriptors.forEach { inputDescriptor ->
        catchingUnwrapped {
            inputDescriptor.extractConsentData()
        }.onSuccess { (representation, scheme, attributes) ->
            RequestedCredentialPreview(
                scheme = scheme,
                representation = representation,
                attributes = attributes
            )
        }.onFailure {
            onError(it)
        }
    }
}

@Composable
fun DcqlRequestPreview(
    presentationRequest: CredentialPresentationRequest.DCQLRequest,
    onError: (Throwable) -> Unit,
) {
    if(presentationRequest.dcqlQuery.requestedCredentialSetQueries.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val credentialSetQuery = presentationRequest.dcqlQuery.requestedCredentialSetQueries.first()

    if(credentialSetQuery.options.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val requestedCredentialCombination = credentialSetQuery.options.first()

    requestedCredentialCombination.forEach { credentialQueryIdentifier ->
        val credentialQuery = presentationRequest.dcqlQuery.credentials.find {
            it.id == credentialQueryIdentifier
        }
        if(credentialQuery == null) {
            return onError(IllegalArgumentException(stringResource(Res.string.error_invalid_dcql_query)))
        }

        val (representation, scheme, attributePaths) = try {
            credentialQuery.extractConsentData()
        } catch (e: Throwable) {
            return onError(e)
        }

        RequestedCredentialPreview(
            scheme = scheme,
            representation = representation,
            attributes = attributePaths.associateWith {
                false
            },
        )
    }
}

@Composable
fun RequestedCredentialPreview(
    scheme: ConstantIndex.CredentialScheme,
    representation: ConstantIndex.CredentialRepresentation,
    attributes: Map<NormalizedJsonPath, Boolean>,
) {
    val schemeName = scheme.uiLabel()
    val format = representation.name
    val list = attributes.mapNotNull { attribute ->
        val resource =
            scheme.getLocalization(NormalizedJsonPath(attribute.key.segments.last())) ?: return@mapNotNull null
        val text =
            catchingUnwrapped { stringResource(resource) }.getOrElse { attribute.key.toString() }
        text to attribute.value
    }.toMap()
    ConsentAttributesSection(
        title = "$schemeName (${format})",
        attributes = list
    )
}
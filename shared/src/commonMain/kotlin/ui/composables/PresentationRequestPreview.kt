package ui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.dif.InputDescriptor
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_invalid_dcql_query
import at.asitplus.wallet.app.common.DcqlConsentData
import at.asitplus.wallet.app.common.PresentationExchangeConsentData
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.CredentialPresentationRequest.DCQLRequest
import at.asitplus.wallet.lib.data.CredentialPresentationRequest.PresentationExchangeRequest
import at.asitplus.wallet.lib.data.CredentialScheme
import data.credentials.JsonClaimReference
import data.credentials.MdocClaimReference
import data.credentials.jwtClaimLabel
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PresentationRequestPreview(
    presentationRequest: CredentialPresentationRequest,
    onError: (Throwable) -> Unit,
) {
    when (presentationRequest) {
        is DCQLRequest -> DcqlRequestPreview(presentationRequest, onError)
        is PresentationExchangeRequest -> PresentationExchangeRequestPreview(presentationRequest, onError)
    }
}

@Composable
fun PresentationExchangeRequestPreview(
    presentationRequest: PresentationExchangeRequest,
    onError: (Throwable) -> Unit,
) {
    InputDescriptorPreview(
        inputDescriptors = presentationRequest.presentationDefinition.inputDescriptors.toList(),
        onError = onError
    )
}

@Composable
fun InputDescriptorPreview(
    inputDescriptors: List<InputDescriptor>,
    onError: (Throwable) -> Unit,
) {
    // Resolved in a coroutine: scheme resolution may fetch type metadata (from the persistent
    // cache or remotely) when the in-memory scheme index is still cold, e.g. right after the
    // iOS identity provider extension process started for a DC API request.
    val consentData by produceState<List<PresentationExchangeConsentData>?>(null, inputDescriptors) {
        value = inputDescriptors.mapNotNull { inputDescriptor ->
            catchingUnwrapped { inputDescriptor.extractConsentData() }
                .onFailure(onError)
                .getOrNull()
        }
    }
    consentData?.forEach { (representation, scheme, attributes) ->
        RequestedCredentialPreview(
            scheme = scheme,
            representation = representation,
            attributes = attributes.mapKeys { it.key }
        )
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DcqlRequestPreview(
    presentationRequest: DCQLRequest,
    onError: (Throwable) -> Unit,
) {
    if (presentationRequest.dcqlQuery.requestedCredentialSetQueries.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val credentialSetQuery = presentationRequest.dcqlQuery.requestedCredentialSetQueries.first()

    if (credentialSetQuery.options.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val requestedCredentialCombination = credentialSetQuery.options.first()

    val invalidQueryMessage = stringResource(Res.string.error_invalid_dcql_query)
    // Resolved in a coroutine: see InputDescriptorPreview.
    val consentData by produceState<List<DcqlConsentData>?>(null, presentationRequest) {
        value = catchingUnwrapped {
            requestedCredentialCombination.map { credentialQueryIdentifier ->
                val credentialQuery = presentationRequest.dcqlQuery.credentials.find {
                    it.id == credentialQueryIdentifier
                } ?: throw IllegalArgumentException(invalidQueryMessage)
                credentialQuery.extractConsentData()
            }
        }.onFailure(onError).getOrNull()
    }

    consentData?.forEach { (representation, scheme, attributePaths) ->
        RequestedCredentialPreview(
            scheme = scheme,
            representation = representation,
            attributes = attributePaths?.map {
                when (it) {
                    is MdocClaimReference -> NormalizedJsonPath() + it.namespace + it.claimName
                    is JsonClaimReference -> it.normalizedJsonPath
                    null -> null
                }
            }?.associateWith { false },
        )
    }
}

@Composable
fun RequestedCredentialPreview(
    scheme: CredentialScheme,
    representation: ConstantIndex.CredentialRepresentation,
    attributes: Map<NormalizedJsonPath?, Boolean>?,
) {
    val schemeName = scheme.uiLabel()
    val format = representation.name
    val localizations = attributes?.let { claimReferences ->
        val otherClaims = claimReferences.count {
            it.key == null
        }
        val singleClaimReferences = claimReferences.filter {
            it.key != null
        }.mapKeys {
            it.key!!
        }
        otherClaims to singleClaimReferences.mapKeys { (path, _) ->
            catchingUnwrapped {
                scheme.getLocalization(path)
                    ?: representation.getMetadataLocalization(path)?.let { stringResource(it) }
                    ?: path.toShorthandNameSegmentNotationWherePossible().removePrefix("$.")
            }.getOrElse { path.toShorthandNameSegmentNotationWherePossible().removePrefix("$.") }
        }
    }
    ConsentAttributesSection(
        title = "$schemeName (${format})",
        attributes = localizations
    )
}

fun ConstantIndex.CredentialRepresentation.getMetadataLocalization(path: NormalizedJsonPath): StringResource? {
    val firstSegment = path.segments.firstOrNull()?.let {
        it as? NormalizedJsonPathSegment.NameSegment
    } ?: return null
    return if (this == ConstantIndex.CredentialRepresentation.ISO_MDOC) null else jwtClaimLabel(firstSegment.memberName)
}

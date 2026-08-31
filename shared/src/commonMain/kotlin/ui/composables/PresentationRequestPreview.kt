package ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_invalid_dcql_query
import at.asitplus.valera.resources.text_label_intent_to_retain
import at.asitplus.wallet.app.common.DcqlConsentData
import at.asitplus.wallet.app.common.IsoDeviceRequestConsentData
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import at.asitplus.wallet.lib.data.CredentialPresentationRequest.DCQLRequest
import at.asitplus.wallet.lib.data.CredentialScheme
import data.credentials.JsonClaimReference
import data.credentials.MdocClaimReference
import data.credentials.jwtClaimLabel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun PresentationRequestPreview(
    presentationRequest: CredentialPresentationRequest,
    onError: (Throwable) -> Unit,
) {
    when (presentationRequest) {
        is DCQLRequest -> DcqlRequestPreview(presentationRequest, onError)
        is CredentialPresentationRequest.IsoDeviceRetrieval -> IsoDeviceRequestPreview(presentationRequest, onError)
        else -> LaunchedEffect(presentationRequest) {
            onError(UnsupportedOperationException("Unsupported presentation request: ${presentationRequest::class.simpleName}"))
        }
    }
}

@Composable
fun IsoDeviceRequestPreview(
    presentationRequest: CredentialPresentationRequest.IsoDeviceRetrieval,
    onError: (Throwable) -> Unit,
) {
    // Resolved in a coroutine: scheme resolution may fetch type metadata (from the persistent cache or remotely)
    // when the in-memory scheme index is still cold, e.g. right after the iOS identity provider extension process
    // started for a DC API request. Resolved per document request, so one unresolvable document type reports its
    // own error instead of blanking the preview for every other document in the same request.
    val consentData by produceState<List<IsoDeviceRequestConsentData>?>(null, presentationRequest) {
        value = presentationRequest.deviceRequest.docRequests.mapNotNull { docRequest ->
            withContext(Dispatchers.Default) {
                catchingUnwrapped { docRequest.extractConsentData() }
            }.onFailure(onError).getOrNull()
        }
    }
    if (consentData == null) {
        PresentationRequestLoadingIndicator()
    }
    consentData?.forEach { request ->
        RequestedCredentialPreview(
            scheme = request.scheme,
            representation = ConstantIndex.CredentialRepresentation.ISO_MDOC,
            attributes = request.attributes.associateWith { false },
            intendsToRetain = request::intendsToRetain,
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
    // Metadata resolution may involve I/O, so keep it outside the composition dispatcher.
    val consentData by produceState<List<DcqlConsentData>?>(null, presentationRequest) {
        value = withContext(Dispatchers.Default) {
            catchingUnwrapped {
                requestedCredentialCombination.map { credentialQueryIdentifier ->
                    val credentialQuery = presentationRequest.dcqlQuery.credentials.find {
                        it.id == credentialQueryIdentifier
                    } ?: throw IllegalArgumentException(invalidQueryMessage)
                    credentialQuery.extractConsentData()
                }
            }
        }.onFailure(onError).getOrNull()
    }

    if (consentData == null) {
        PresentationRequestLoadingIndicator()
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
internal fun PresentationRequestLoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
fun RequestedCredentialPreview(
    scheme: CredentialScheme,
    representation: ConstantIndex.CredentialRepresentation,
    attributes: Map<NormalizedJsonPath?, Boolean>?,
    intendsToRetain: (NormalizedJsonPath) -> Boolean = { false },
) {
    val schemeName = scheme.uiLabel()
    val format = representation.name
    val intentToRetainText = stringResource(Res.string.text_label_intent_to_retain)
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
            val label = catchingUnwrapped {
                scheme.getLocalization(path)
                    ?: representation.getMetadataLocalization(path)?.let { stringResource(it) }
                    ?: path.toShorthandNameSegmentNotationWherePossible().removePrefix("$.")
            }.getOrElse { path.toShorthandNameSegmentNotationWherePossible().removePrefix("$.") }
            if (intendsToRetain(path)) "$label ($intentToRetainText)" else label
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

package ui.composables

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.error_complex_dcql_query
import at.asitplus.valera.resources.error_invalid_dcql_query
import at.asitplus.wallet.app.common.extractConsentData
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.uiLabel
import at.asitplus.wallet.lib.data.ConstantIndex
import at.asitplus.wallet.lib.data.CredentialPresentationRequest
import data.credentials.JwtClaimDefinition
import data.credentials.JwtClaimDefinitionTranslator
import data.credentials.MdocClaimReference
import data.credentials.SdJwtClaimReference
import org.jetbrains.compose.resources.StringResource
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
                attributes = attributes.mapKeys {
                    it.key as? NormalizedJsonPath
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
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
    if (presentationRequest.dcqlQuery.requestedCredentialSetQueries.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val credentialSetQuery = presentationRequest.dcqlQuery.requestedCredentialSetQueries.first()

    if (credentialSetQuery.options.size != 1) {
        return onError(UnsupportedOperationException(stringResource(Res.string.error_complex_dcql_query)))
    }
    val requestedCredentialCombination = credentialSetQuery.options.first()

    requestedCredentialCombination.forEach { credentialQueryIdentifier ->
        val credentialQuery = presentationRequest.dcqlQuery.credentials.find {
            it.id == credentialQueryIdentifier
        }
        if (credentialQuery == null) {
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
            attributes = attributePaths?.map {
                when (it) {
                    is MdocClaimReference -> NormalizedJsonPath() + it.namespace + it.claimName
                    is SdJwtClaimReference -> it.normalizedJsonPath
                    null -> null
                }
            }?.associateWith { false },
        )
    }
}

@Composable
fun RequestedCredentialPreview(
    scheme: ConstantIndex.CredentialScheme,
    representation: ConstantIndex.CredentialRepresentation,
//    attributes: Map<SingleClaimReference?, Boolean>?,
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
                (scheme.getLocalization(path) ?: representation.getMetadataLocalization(path))
                    ?.let { stringResource(it) }
                    ?: path.toString()
            }.getOrElse { path.toString() }
        }
    }
    ConsentAttributesSection(
        title = "$schemeName (${format})",
        attributes = localizations
    )
}

private fun ConstantIndex.CredentialRepresentation.getMetadataLocalization(path: NormalizedJsonPath): StringResource? {
    val firstSegment = path.segments.firstOrNull()?.let {
        it as? NormalizedJsonPathSegment.NameSegment
    } ?: return null
    val jwtClaimDefinition = JwtClaimDefinition.valueOfClaimNameOrNull(firstSegment.memberName) ?: return null
    return JwtClaimDefinitionTranslator().translate(jwtClaimDefinition)
}
package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.dcql.DCQLClaimsQueryResult
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.wallet.app.common.TrustListService
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult.*
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json.leafNodeList
import at.asitplus.wallet.lib.agent.SdJwtDecoded
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialToJsonConverter.toJsonElement
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.credentials.labeledPresentationAttributes
import data.credentials.toCredentialAdapter
import data.credentials.CredentialAdapter
import data.Attribute
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.JsonObject
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.json.buildJsonObject
import org.koin.compose.koinInject
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessValidationStateUiModel
import ui.models.ResolvedCredential
import ui.models.toFallbackResolvedCredential
import ui.models.toResolvedCredential



@Composable
fun DCQLCredentialQuerySubmissionSelectionOption(
    isSelected: Boolean,
    onToggleSelection: (() -> Unit)?,
    modifier: Modifier = Modifier,
    decodeToBitmap: ImageDecoder = koinInject(),
    allowMultiSelection: Boolean,
    credential: SubjectCredentialStore.StoreEntry,
    matchingResult: KmmResult<DCQLCredentialQueryMatchingResult>,
    freshnessState: StateFlow<CredentialFreshnessValidationStateUiModel>,
    trustListService: TrustListService,
    onLoadingChanged: (Boolean) -> Unit = {},
    onError: (Throwable) -> Unit = {},
) {
    val credentialFreshnessValidationState by freshnessState.collectAsState()
    val previewState by produceState<CredentialPreviewState>(
        CredentialPreviewState.Loading,
        credential,
        matchingResult,
    ) {
        value = withContext(Dispatchers.Default) {
            catchingUnwrapped {
                val resolutionResult = catchingUnwrapped { credential.toResolvedCredential() }
                val resolvedCredential = resolutionResult
                    .getOrElse { credential.toFallbackResolvedCredential() }
                val genericAttributeList = credential.presentationAttributes(matchingResult)
                val credentialAdapter = credential.toCredentialAdapter(resolvedCredential.scheme) {
                    decodeToBitmap(it)
                }
                CredentialPreviewState.Ready(
                    resolvedCredential = resolvedCredential,
                    credentialAdapter = credentialAdapter,
                    labeledAttributes = credentialAdapter.labeledPresentationAttributes(genericAttributeList),
                    loadingError = resolutionResult.exceptionOrNull(),
                )
            }.fold(
                onSuccess = { it },
                onFailure = { CredentialPreviewState.Error(it) },
            )
        }
    }
    LaunchedEffect(previewState) {
        onLoadingChanged(previewState is CredentialPreviewState.Loading)
        when (val state = previewState) {
            CredentialPreviewState.Loading -> Unit
            is CredentialPreviewState.Ready -> state.loadingError?.let(onError)
            is CredentialPreviewState.Error -> onError(state.throwable)
        }
    }
    val preview = previewState as? CredentialPreviewState.Ready ?: return
    val displayCredential = preview.resolvedCredential

    val trustState by trustListService
        .observeTrustStateForEntry(flowOf(displayCredential))
        .collectAsState(initial = TrustState.EVALUATING)

    CredentialSelectionCardLayout(
        isError = matchingResult.isFailure || when (val it = credentialFreshnessValidationState) {
            is CredentialFreshnessValidationStateUiModel.Done -> !it.credentialFreshnessSummary.isNotBad
            CredentialFreshnessValidationStateUiModel.Loading -> false
        },
        onClick = onToggleSelection.takeIf {
            matchingResult.isSuccess
        } ?: {}.takeIf {
            isSelected // make it look like it is enabled as long as it is selected
        },
        isSelected = isSelected,
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = credentialFreshnessValidationState,
            matchingException = matchingResult.exceptionOrNull(),
            credential = displayCredential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = allowMultiSelection,
        )

        TrustStatusBanner(
            trustState = trustState,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        CredentialSummaryCardContent(preview.credentialAdapter)
        matchingResult.exceptionOrNull()?.message?.let {
            BigErrorText(it)
        }
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        AnimatedVisibility(!isSelected) {
            Text(preview.labeledAttributes.joinToString(", ") { it.first })
        }
        AnimatedVisibility(isSelected) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxWidth().align(Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                preview.labeledAttributes.forEach {
                    LabeledAttribute(
                        label = it.first,
                        attribute = it.second,
                    )
                }
            }

        }
    }
}

private sealed interface CredentialPreviewState {
    data object Loading : CredentialPreviewState

    data class Ready(
        val resolvedCredential: ResolvedCredential,
        val credentialAdapter: CredentialAdapter,
        val labeledAttributes: List<Pair<String, Attribute>>,
        val loadingError: Throwable?,
    ) : CredentialPreviewState

    data class Error(val throwable: Throwable) : CredentialPreviewState
}

private fun SubjectCredentialStore.StoreEntry.presentationAttributes(
    matchingResult: KmmResult<DCQLCredentialQueryMatchingResult>,
): List<Pair<NormalizedJsonPath, Any>> = when (val result = matchingResult.getOrNull()) {
    null,
    AllClaimsMatchingResult -> allClaims().leafNodeList().map {
        it.normalizedJsonPath to it.value
    }

    // only claims that are not selectively disclosable will be presented
    AllMandatoryClaimsMatchingResult -> mandatoryClaims().leafNodeList().map {
        it.normalizedJsonPath to it.value
    }

    is ClaimsQueryResults -> result.claimsQueryResults.flatMap {
        when (it) {
            is DCQLClaimsQueryResult.IsoMdocResult -> listOf(
                NormalizedJsonPath() + it.namespace + it.claimName to it.claimValue,
            )

            is DCQLClaimsQueryResult.JsonResult -> it.nodeList.map { node ->
                node.normalizedJsonPath to node.value
            }
        }
    }
}

/** Claims that are presented even without being requested, because they are not selectively disclosable. */
private fun SubjectCredentialStore.StoreEntry.mandatoryClaims() = when (this) {
    // every mdoc data element is selectively disclosable
    is SubjectCredentialStore.StoreEntry.Iso -> buildJsonObject { }

    is SubjectCredentialStore.StoreEntry.SdJwt -> SdJwtSigned.parseCatching(vcSerialized).getOrNull()
        ?.jws?.getPayload<JsonObject>()?.getOrNull()?.withoutSdMachinery()
        ?: buildJsonObject { }

    is SubjectCredentialStore.StoreEntry.Vc -> allClaims()
}

private fun JsonObject.withoutSdMachinery(): JsonObject = JsonObject(
    filterKeys { it != "_sd" && it != "_sd_alg" }.mapValues { (_, value) ->
        (value as? JsonObject)?.withoutSdMachinery() ?: value
    }
)

private fun SubjectCredentialStore.StoreEntry.allClaims() = when (this) {
    is SubjectCredentialStore.StoreEntry.Iso -> buildJsonObject {
        issuerSigned.namespaces?.forEach {
            put(it.key, buildJsonObject {
                it.value.entries.map { it.value }.forEach {
                    put(it.elementIdentifier, it.elementValue.toJsonElement())
                }
            })
        }
    }

    is SubjectCredentialStore.StoreEntry.SdJwt -> SdJwtSigned.parseCatching(vcSerialized).getOrNull()
        ?.let { SdJwtDecoded(it).reconstructedJsonObject } ?: buildJsonObject {
        disclosures.forEach { disclosure ->
            disclosure.value?.claimValue?.let { put(disclosure.key, it) }
        }
    }

    is SubjectCredentialStore.StoreEntry.Vc -> vc.vc.credentialSubject
}

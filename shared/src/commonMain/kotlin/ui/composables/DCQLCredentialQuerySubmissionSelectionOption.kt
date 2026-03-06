package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.dcql.DCQLClaimsQueryResult
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.app.common.domain.platform.ImageDecoder
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json.leafNodeList
import at.asitplus.wallet.lib.agent.SdJwtDecoded
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialToJsonConverter.toJsonElement
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.credentials.FallbackCredentialAdapter
import data.credentials.toCredentialAdapter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessSummaryModelEvaluator
import ui.models.CredentialFreshnessValidationStateUiModel


@Composable
fun DCQLCredentialQuerySubmissionSelectionOption(
    isSelected: Boolean,
    onToggleSelection: (() -> Unit)?,
    option: DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>,
    modifier: Modifier = Modifier,
    decodeToBitmap: ImageDecoder = koinInject(),
    checkCredentialFreshness: CredentialFreshnessSummaryModelEvaluator = koinInject(),
    allowMultiSelection: Boolean,
) {
    val credentialFreshnessValidationState by produceState(
        CredentialFreshnessValidationStateUiModel.Loading as CredentialFreshnessValidationStateUiModel,
        option.credential
    ) {
        value = CredentialFreshnessValidationStateUiModel.Loading
        value = CredentialFreshnessValidationStateUiModel.Done(checkCredentialFreshness(option.credential))
    }

    val credential = option.credential
    val matchingResult = option.matchingResult

    val genericAttributeList: List<Pair<NormalizedJsonPath, Any>> = when (matchingResult) {
        DCQLCredentialQueryMatchingResult.AllClaimsMatchingResult -> credential.allClaims().leafNodeList().map {
            it.normalizedJsonPath to it.value
        }

        is DCQLCredentialQueryMatchingResult.ClaimsQueryResults -> matchingResult.claimsQueryResults.flatMap {
            when (it) {
                is DCQLClaimsQueryResult.IsoMdocResult -> listOf(
                    NormalizedJsonPath() + it.namespace + it.claimName to it.claimValue,
                )

                is DCQLClaimsQueryResult.JsonResult -> it.nodeList.map {
                    it.normalizedJsonPath to it.value
                }
            }
        }
    }

    val credentialAdapter = credential.toCredentialAdapter {
        decodeToBitmap(it)
    } ?: FallbackCredentialAdapter(genericAttributeList, credential)
    val labeledAttributes = genericAttributeList.mapNotNull { (key, value) ->
        credentialAdapter.getAttribute(key)?.let { attribute ->
            key.segments.lastOrNull()?.let {
                credential.scheme?.getLocalization(NormalizedJsonPath(it))?.let {
                    stringResource(it)
                }
            }?.let { it to attribute }
        }
    }.sortedBy {
        it.first
    }

    CredentialSelectionCardLayout(
        credentialFreshnessValidationState = credentialFreshnessValidationState,
        onClick = onToggleSelection ?: {}.takeIf {
            isSelected // make it look like it is enabled as long as it is selected
        },
        isSelected = isSelected,
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = credentialFreshnessValidationState,
            credential = credential,
            modifier = Modifier.fillMaxWidth(),
            allowMultiSelection = allowMultiSelection,
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = {
                decodeToBitmap(it)
            },
        )
        HorizontalDivider(modifier = Modifier.fillMaxWidth())
        AnimatedVisibility(!isSelected) {
            Text(labeledAttributes.joinToString(", ") { it.first })
        }
        AnimatedVisibility(isSelected) {
            Column(
                modifier = Modifier.padding(8.dp).fillMaxWidth().align(Alignment.Start),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                labeledAttributes.forEach {
                    LabeledAttribute(
                        label = it.first,
                        attribute = it.second,
                    )
                }
            }
        }
    }
}

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

    is SubjectCredentialStore.StoreEntry.Vc -> vckJsonSerializer.encodeToJsonElement(vc)
}


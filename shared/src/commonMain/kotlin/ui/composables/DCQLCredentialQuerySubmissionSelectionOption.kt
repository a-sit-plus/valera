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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.dcql.DCQLClaimsQueryResult
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.agent.representation
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json.leafNodeList
import at.asitplus.wallet.lib.agent.SdJwtDecoded
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialToJsonConverter.toJsonElement
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.Attribute
import data.credentials.CredentialAdapter
import data.credentials.toCredentialAdapter
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.compose.resources.stringResource
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.CredentialFreshnessValidationStateUiModel


@Composable
fun DCQLCredentialQuerySubmissionSelectionOption(
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    option: DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>,
    checkCredentialFreshness: suspend (SubjectCredentialStore.StoreEntry) -> CredentialFreshnessSummaryUiModel,
    decodeToBitmap: (ByteArray) -> Result<ImageBitmap>,
    modifier: Modifier = Modifier,
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

    val credentialAdapter = credential.toCredentialAdapter(decodeToBitmap) ?: object : CredentialAdapter() {
        // trying our best to map the values to attributes
        private val mapping = genericAttributeList.toMap()

        override fun getAttribute(path: NormalizedJsonPath): Attribute? {
            return mapping[path]?.let {
                Attribute.fromValue(it)
            }
        }

        override val representation = credential.representation
        override val scheme = credential.scheme!!
    }
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
        onClick = onToggleSelection,
        isSelected = isSelected,
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
            credentialFreshnessValidationState = credentialFreshnessValidationState,
            credential = credential,
            modifier = Modifier.fillMaxWidth()
        )
        CredentialSummaryCardContent(
            credential = credential,
            decodeToBitmap = decodeToBitmap,
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

    is SubjectCredentialStore.StoreEntry.SdJwt -> SdJwtSigned.parse(vcSerialized)
        ?.let { SdJwtDecoded(it).reconstructedJsonObject } ?: buildJsonObject {
        disclosures.forEach { disclosure ->
            disclosure.value?.claimValue?.let { put(disclosure.key, it) }
        }
    }

    is SubjectCredentialStore.StoreEntry.Vc -> vckJsonSerializer.encodeToJsonElement(vc)
}


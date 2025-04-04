package ui.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.openid.dcql.DCQLClaimsQueryResult
import at.asitplus.openid.dcql.DCQLCredentialQueryMatchingResult
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.jsonpath.core.plus
import at.asitplus.wallet.app.common.thirdParty.at.asitplus.wallet.lib.data.getLocalization
import at.asitplus.wallet.app.common.thirdParty.kotlinx.serialization.json.leafNodeList
import at.asitplus.wallet.lib.agent.SdJwtValidator
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.CredentialToJsonConverter.toJsonElement
import at.asitplus.wallet.lib.data.vckJsonSerializer
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.Attribute
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import org.jetbrains.compose.resources.stringResource
import ui.composables.credentials.CredentialSelectionCardHeader
import ui.composables.credentials.CredentialSelectionCardLayout
import ui.composables.credentials.CredentialSummaryCardContent


@Composable
fun DCQLCredentialQuerySubmissionSelectionOption(
    isSelected: Boolean,
    onToggleSelection: () -> Unit,
    option: DCQLCredentialSubmissionOption<SubjectCredentialStore.StoreEntry>,
    decodeToBitmap: (ByteArray) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
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
    val labeledAttributes = genericAttributeList.mapNotNull { (key, value) ->
        Attribute.fromValue(value)?.let { attribute ->
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
        onClick = onToggleSelection,
        isSelected = isSelected,
        modifier = modifier,
    ) {
        CredentialSelectionCardHeader(
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
        ?.let { SdJwtValidator(it).reconstructedJsonObject } ?: buildJsonObject {
        disclosures.forEach { disclosure ->
            disclosure.value?.claimValue?.let { put(disclosure.key, it) }
        }
    }

    is SubjectCredentialStore.StoreEntry.Vc -> vckJsonSerializer.encodeToJsonElement(vc)
}


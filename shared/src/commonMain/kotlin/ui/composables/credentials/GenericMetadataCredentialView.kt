package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.app.common.memberName
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.Attribute
import data.credentials.metadataLabel
import data.credentials.toGenericAttributeList
import ui.composables.AttributeRepresentation
import ui.composables.LabeledContent
import ui.composables.PersonAttributeDetailCardHeading

/**
 * Renders a credential that has no bespoke view by listing its disclosed claims, labelled from the resolved
 * scheme's SD-JWT VC Type Metadata where available, grouped by the first claim-path segment.
 */
@Composable
fun GenericMetadataCredentialView(
    storeEntry: SubjectCredentialStore.StoreEntry,
) {
    @Suppress("DEPRECATION")
    val scheme = storeEntry.scheme ?: return
    val grouped = remember(storeEntry) {
        storeEntry.toGenericAttributeList()
            // status and cnf are shown in their own cards (see GenericCredentialSummaryCardContent).
            .filterNot { it.first.memberName(0) in HIDDEN_TOP_LEVEL_CLAIMS }
            .sortedBy { it.first.toString() }
            .groupBy { it.first.memberName(0) ?: it.first.toString() }
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
        grouped.forEach { (groupKey, items) ->
            // Show a group header only when there are multiple groups and this one has nested sub-claims
            // (e.g. address.region, address.locality). A single group would just duplicate the credential heading.
            if (grouped.size > 1 && items.any { it.first.segments.size > 1 }) {
                val groupLabel = scheme.metadataLabel(NormalizedJsonPath() + groupKey) ?: groupKey
                PersonAttributeDetailCardHeading(
                    title = groupLabel,
                    iconText = groupLabel.take(2).uppercase(),
                )
            }
            items.forEach { (path, value) ->
                Attribute.fromValue(value)?.let { attribute ->
                    LabeledContent(
                        label = scheme.metadataLabel(path) ?: path.genericLabel(),
                        modifier = Modifier.padding(bottom = 8.dp),
                    ) {
                        AttributeRepresentation(attribute)
                    }
                }
            }
        }
    }
}

private fun NormalizedJsonPath.genericLabel(): String =
    (0 until segments.size).mapNotNull { memberName(it) }.joinToString(".").ifEmpty { toString() }

/** Registered JWT claims rendered in their own dedicated cards rather than the attribute list. */
private val HIDDEN_TOP_LEVEL_CLAIMS = setOf("status", "cnf", "vct", "iat", "iss", "nbf", "exp", "sub")

package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.lib.data.ConstantIndex
import data.PersonalDataCategory
import data.credentials.CredentialAdapter
import data.credentials.CredentialAttributeCategorization
import data.credentials.CredentialAttributeTranslator
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeRepresentation
import ui.composables.LabeledContent


@Composable
fun GenericDataCardContent(
    credentialScheme: ConstantIndex.CredentialScheme,
    attributes: List<Pair<NormalizedJsonPath, @Composable () -> Unit>>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        attributes.mapIndexed { index, it ->
            LabeledContent(
                label = CredentialAttributeTranslator[credentialScheme]
                    ?.translate(it.first)?.let {
                        stringResource(it)
                    } ?: it.first.toString(),
                content = it.second,
                modifier = if (index == attributes.lastIndex) {
                    Modifier
                } else {
                    Modifier.padding(bottom = 8.dp)
                }
            )
        }
    }
}

fun getGenericAttributeRepresentations(
    credentialScheme: ConstantIndex.CredentialScheme,
    personalDataCategory: PersonalDataCategory,
    credentialAdapter: CredentialAdapter,
): List<Pair<NormalizedJsonPath, @Composable () -> Unit>> {
    val attributeCategorization = CredentialAttributeCategorization.load(
        credentialScheme,
        credentialAdapter.representation
    )[personalDataCategory]
        ?: throw IllegalStateException("credentialAttributeCategorization")

    val attributes = attributeCategorization.flatMap { virtualCategory ->
        virtualCategory.second?.map { virtualCategory.first + it } ?: listOf(virtualCategory.first)
    }

    return attributes.mapNotNull { attributeName ->
        credentialAdapter.getAttribute(attributeName)?.let { attribute ->
            attributeName to {
                AttributeRepresentation(attribute)
            }
        }
    }
}
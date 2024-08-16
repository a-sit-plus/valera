package ui.composables

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.wallet.idaustria.IdAustriaScheme
import at.asitplus.wallet.lib.data.ConstantIndex
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.dictionary_no
import composewalletapp.shared.generated.resources.dictionary_yes
import data.Attribute
import data.PersonalDataCategory
import data.credentialAttributeCategorization
import data.credentials.CredentialAdapter
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource


@Composable
fun AttributeRepresentation(attribute: Attribute) {
    when (attribute) {
        is Attribute.StringAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.BooleanAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.DateAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.GenderAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.ImageAttribute -> AttributeRepresentation(attribute.value)
        is Attribute.IntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
        is Attribute.UnsignedIntegerAttribute -> TODO() // AttributeRepresentation(attribute.value)
    }
}


@Composable
fun AttributeRepresentation(value: String) {
    Text(value)
}


@Composable
fun AttributeRepresentation(value: LocalDate) {
    Text(value.run { "$dayOfMonth.$monthNumber.$year" })
}


@Composable
fun AttributeRepresentation(
    value: ImageBitmap,
) {
    Image(
        bitmap = value,
        contentDescription = null,
    )
}


@Composable
fun AttributeRepresentation(
    value: Boolean,
) {
    AttributeRepresentation(
        if (value) {
            stringResource(Res.string.dictionary_yes)
        } else {
            stringResource(Res.string.dictionary_no)
        }
    )
}

fun getGenericAttributeRepresentations(
    credentialScheme: ConstantIndex.CredentialScheme,
    personalDataCategory: PersonalDataCategory,
    credentialAdapter: CredentialAdapter,
): List<Pair<NormalizedJsonPath, @Composable () -> Unit>> {
    val attributeNames = credentialAttributeCategorization.get(credentialScheme)
        ?.get(personalDataCategory)
        ?: throw IllegalStateException("credentialAttributeCategorization")

    return attributeNames.mapNotNull { attributeName ->
        credentialAdapter.getAttribute(attributeName)?.let { attribute ->
            attributeName to {
                AttributeRepresentation(attribute)
            }
        }
    }
}
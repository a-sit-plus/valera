package ui.composables.credentials

import ExpandButtonUpDown
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.wallet.idaustria.IdAustriaScheme
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_portrait
import data.AttributeTranslator
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeRepresentation

@Composable
fun IdAustriaIdentityDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    imageDecoder: (ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    var isExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val portraitBitmap = remember {
        credentialAdapter.portrait?.let(imageDecoder)
    }

    IdAustriaIdentityDataCard(
        isExpanded = isExpanded,
        onChangeIsExpanded = { isExpanded = !isExpanded },
        credentialAdapter = credentialAdapter,
        portraitBitmap = portraitBitmap,
        modifier = modifier,
    )
}

@Composable
fun IdAustriaIdentityDataCard(
    isExpanded: Boolean,
    onChangeIsExpanded: (Boolean) -> Unit,
    credentialAdapter: IdAustriaCredentialAdapter,
    portraitBitmap: ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    ElevatedCard(
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(bottom = 16.dp, start = 16.dp, end = 16.dp).fillMaxWidth()
        ) {
            CredentialDetailCardHeader(
                iconText = PersonalDataCategory.IdentityData.iconText,
                title = PersonalDataCategory.IdentityData.categoryTitle,
                isExpanded = isExpanded,
                onChangeIsExpanded = onChangeIsExpanded,
            )
            if (isExpanded) {
                GenericDataCardContent(
                    listOfNotNull<Pair<String, @Composable () -> Unit>>(
                        IdAustriaScheme.Attributes.FIRSTNAME to {
                            AttributeRepresentation(credentialAdapter.givenName)
                        },
                        IdAustriaScheme.Attributes.LASTNAME to {
                            AttributeRepresentation(credentialAdapter.familyName)
                        },
                        IdAustriaScheme.Attributes.DATE_OF_BIRTH to {
                            AttributeRepresentation(credentialAdapter.dateOfBirth)
                        },
                        portraitBitmap?.let {
                            IdAustriaScheme.Attributes.PORTRAIT to {
                                AttributeRepresentation(it)
                            }
                        },
                    ).map {
                        val translation =
                            AttributeTranslator(IdAustriaScheme).translate(it.first)?.let {
                                stringResource(it)
                            } ?: it.first
                        translation to it.second
                    },
                )
            } else {
                IdAustriaIdentityDataCardContent(
                    credentialAdapter = credentialAdapter,
                    portraitBitmap = portraitBitmap,
                )
            }
        }
    }
}

@Composable
fun IdAustriaIdentityDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    portraitBitmap: ImageBitmap?,
) {
    var columnSize by remember { mutableStateOf(Size.Zero) }
    Row(horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
            columnSize = layoutCoordinates.size.toSize()
        }) {
        portraitBitmap?.let {
            // source: https://stackoverflow.com/questions/69455135/does-jetpack-compose-have-maxheight-and-maxwidth-like-xml
            // weird way to get "at most 1/4th of max width"
            val maxWidth = LocalDensity.current.run { (0.25f * columnSize.width).toDp() }
            Image(
                bitmap = portraitBitmap,
                contentDescription = stringResource(Res.string.content_description_portrait),
                modifier = Modifier.widthIn(0.dp, maxWidth).padding(end = 16.dp),
                contentScale = ContentScale.FillWidth,
            )
        }
        val textGap = 4.dp
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = listOfNotNull(
                    credentialAdapter.givenName, credentialAdapter.familyName
                ).joinToString(" "),
                modifier = Modifier.padding(bottom = textGap),
            )
            Text(credentialAdapter.dateOfBirth.run { "$dayOfMonth.$monthNumber.$year" })
        }
    }
}
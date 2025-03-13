package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.content_description_portrait
import at.asitplus.wallet.idaustria.IdAustriaScheme
import data.PersonalDataCategory
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource
import ui.composables.AttributeRepresentation

@Composable
fun IdAustriaCredentialIdentityDataCard(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier,
) {
    CredentialDetailCard(
        credentialScheme = IdAustriaScheme,
        personalDataCategory = PersonalDataCategory.IdentityData,
        credentialAdapter = credentialAdapter,
        modifier = modifier,
    ) {
        IdAustriaCredentialIdentityDataCardContent(
            credentialAdapter = credentialAdapter,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
        )
    }
}

@Composable
fun IdAustriaCredentialIdentityDataCardContent(
    credentialAdapter: IdAustriaCredentialAdapter,
    modifier: Modifier = Modifier
) {
    var columnSize by remember { mutableStateOf(Size.Zero) }
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
            columnSize = layoutCoordinates.size.toSize()
        },
    ) {
        credentialAdapter.portraitBitmap?.let { portraitBitmap ->
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
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            val spacingModifier = Modifier.padding(bottom = 4.dp)
            AttributeRepresentation(
                value = listOfNotNull(
                    credentialAdapter.givenName, credentialAdapter.familyName
                ).joinToString(" "),
                modifier = spacingModifier,
            )
            credentialAdapter.dateOfBirth?.let { AttributeRepresentation(it) }
        }
    }
}

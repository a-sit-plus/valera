package ui.composables.credentials

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import composewalletapp.shared.generated.resources.Res
import composewalletapp.shared.generated.resources.content_description_portrait
import data.credentials.IdAustriaCredentialAdapter
import org.jetbrains.compose.resources.stringResource

@Composable
fun IdAustriaCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (ByteArray) -> ImageBitmap,
    modifier: Modifier = Modifier,
) {
    val credentialAdapter = remember {
        IdAustriaCredentialAdapter.createFromStoreEntry(credential, decodeImage = decodeImage)
    }

    var columnSize by remember { mutableStateOf(Size.Zero) }
    Row(
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
            columnSize = layoutCoordinates.size.toSize()
        }
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
        val textGap = 4.dp
        Column(
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                text = listOfNotNull(
                    credentialAdapter.givenName,
                    credentialAdapter.familyName
                ).joinToString(" "),
                modifier = Modifier.padding(bottom = textGap),
            )
            Text(credentialAdapter.dateOfBirth.run { "$dayOfMonth.$monthNumber.$year" })
        }
    }
}
package ui.composables.credentials

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.IdAustriaCredentialAdapter

@Composable
fun IdAustriaCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    decodeImage: (ByteArray) -> ImageBitmap,
) {
    val credentialAdapter = remember {
        IdAustriaCredentialAdapter.createFromStoreEntry(credential, decodeImage = decodeImage)
    }

    IdAustriaCredentialIdentityDataCardContent(
        credentialAdapter = credentialAdapter,
    )
//
//    var columnSize by remember { mutableStateOf(Size.Zero) }
//    Row(
//        horizontalArrangement = Arrangement.Start,
//        modifier = Modifier.fillMaxWidth().onGloballyPositioned { layoutCoordinates ->
//            columnSize = layoutCoordinates.size.toSize()
//        }
//    ) {
//        credentialAdapter.portraitBitmap?.let { portraitBitmap ->
//            // source: https://stackoverflow.com/questions/69455135/does-jetpack-compose-have-maxheight-and-maxwidth-like-xml
//            // weird way to get "at most 1/4th of max width"
//            val maxWidth = LocalDensity.current.run { (0.25f * columnSize.width).toDp() }
//            Image(
//                bitmap = portraitBitmap,
//                contentDescription = stringResource(Res.string.content_description_portrait),
//                modifier = Modifier.widthIn(0.dp, maxWidth).padding(end = 16.dp),
//                contentScale = ContentScale.FillWidth,
//            )
//        }
//        val textGap = 4.dp
//        Column(
//            horizontalAlignment = Alignment.Start,
//        ) {
//            Text(
//                text = listOfNotNull(
//                    credentialAdapter.givenName,
//                    credentialAdapter.familyName
//                ).joinToString(" "),
//                modifier = Modifier.padding(bottom = textGap),
//            )
//            Text(credentialAdapter.dateOfBirth.run { "$dayOfMonth.$monthNumber.$year" })
//        }
//    }
}
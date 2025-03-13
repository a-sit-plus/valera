package ui.composables.credentials

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_hide_technical_details
import at.asitplus.valera.resources.button_label_show_technical_details
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import data.credentials.CredentialAdapter.Companion.toComplexJson
import org.jetbrains.compose.resources.stringResource
import ui.composables.Label
import ui.composables.LabeledText
import kotlin.math.min


@Composable
fun GenericCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    var showContent by remember { mutableStateOf(false) }

    val density = LocalDensity.current
    AnimatedVisibility(
        visible = showContent,
        enter = slideInVertically {
            with(density) { -20.dp.roundToPx() }
        } + expandVertically(
            expandFrom = Alignment.Top
        ) + fadeIn(
            initialAlpha = 0.3f
        ),
        exit = slideOutVertically {
            with(density) { 20.dp.roundToPx() }
        } + shrinkVertically(
            shrinkTowards = Alignment.Bottom
        ) + fadeOut(
            targetAlpha = 0f
        )
    ) {
        Column(
            modifier = modifier
        ) {
            when (credential) {
                is SubjectCredentialStore.StoreEntry.Vc -> SingleVcCredentialCardContent(credential = credential)
                is SubjectCredentialStore.StoreEntry.SdJwt -> SingleSdJwtCredentialCardContent(credential = credential)
                is SubjectCredentialStore.StoreEntry.Iso -> SingleIsoCredentialCardContent(credential = credential)
            }
        }
    }
    Column(
        modifier = modifier.fillMaxWidth().clickable(onClick = { showContent = !showContent }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Label(
            label = when (showContent) {
                true -> stringResource(Res.string.button_label_hide_technical_details)
                else -> stringResource(Res.string.button_label_show_technical_details)
            }
        )
        Icon(
            imageVector = when (showContent) {
                true -> Icons.Outlined.ArrowUpward
                else -> Icons.Outlined.ArrowDownward
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary
        )
    }
}

@Composable
private fun SingleVcCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Vc,
) {
    Text(
        credential.vc.vc.credentialSubject.toString().replace("""\[.+]""".toRegex(), "[...]")
            .replace(", ", "\n")
    )

}

@Composable
private fun SingleSdJwtCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.SdJwt,
) {
    credential.allEntries().forEach {
        LabeledText(
            label = it.first,
            text = it.second.run { slice(0..min(lastIndex, 100)) },
        )
    }
}

private fun SubjectCredentialStore.StoreEntry.SdJwt.allEntries(): Collection<Pair<String, String>> =
    (simpleDisclosures() + complexObject()).sortedBy { it.first }.toSet()

private fun SubjectCredentialStore.StoreEntry.SdJwt.complexObject() =
    (toComplexJson()?.entries?.map { it.key to it.value.prettyToString() } ?: listOf())

private fun SubjectCredentialStore.StoreEntry.SdJwt.simpleDisclosures() =
    disclosures.entries.filter { it.value?.claimName != null }.map { it.value!!.claimName!! to it.value!!.claimValue.prettyToString() }

@Composable
private fun SingleIsoCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Iso,
) {
    credential.issuerSigned.namespaces?.forEach { namespace ->
        namespace.value.entries.sortedBy { it.value.elementIdentifier }.forEach { entry ->
            LabeledText(
                text = entry.value.elementValue.prettyToString()
                    .run { slice(0..min(lastIndex, 100)) },
                label = NormalizedJsonPath(
                    NormalizedJsonPathSegment.NameSegment(namespace.key),
                    NormalizedJsonPathSegment.NameSegment(entry.value.elementIdentifier),
                ).toString(),
            )
        }
    }
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}
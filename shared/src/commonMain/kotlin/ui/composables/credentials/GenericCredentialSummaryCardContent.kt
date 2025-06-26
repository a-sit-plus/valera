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
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.button_label_hide_technical_details
import at.asitplus.valera.resources.button_label_show_technical_details
import at.asitplus.valera.resources.section_heading_technical_data_icon_text
import at.asitplus.valera.resources.section_heading_technical_data
import at.asitplus.valera.resources.text_label_valid_from
import at.asitplus.valera.resources.text_label_valid_to
import at.asitplus.valera.resources.text_label_vcType
import at.asitplus.valera.resources.text_label_docType
import at.asitplus.valera.resources.text_label_status_uri
import at.asitplus.valera.resources.text_label_status_idx
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.data.Status
import data.credentials.CredentialAdapter.Companion.toComplexJson
import kotlinx.datetime.Instant
import org.jetbrains.compose.resources.stringResource
import ui.composables.Label
import ui.composables.LabeledContent
import ui.composables.LabeledText
import ui.composables.PersonAttributeDetailCardHeading
import kotlin.math.min


@Composable
fun GenericCredentialSummaryCardContent(
    credential: StoreEntry,
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
                is StoreEntry.Vc -> SingleVcCredentialCardContent(credential)
                is StoreEntry.SdJwt -> SingleSdJwtCredentialCardContent(credential)
                is StoreEntry.Iso -> SingleIsoCredentialCardContent(credential)
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
    credential: StoreEntry.Vc,
) {
    val modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
    ElevatedCard(modifier = Modifier.padding(bottom = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TechnicalMetadataHeader()
            credential.vc.vc.credentialStatus?.let {
                StatusUri(it, modifier)
                StatusIndex(it, modifier)
            }
            ValidFrom(credential.vc.notBefore, modifier)
            credential.vc.expiration?.let {
                ValidUntil(it, modifier)
            }
        }
    }
    Text(
        credential.vc.vc.credentialSubject.toString().replace("""\[.+]""".toRegex(), "[...]")
            .replace(", ", "\n")
    )
}


@Composable
private fun SingleSdJwtCredentialCardContent(
    credential: StoreEntry.SdJwt,
) {
    val modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
    ElevatedCard(modifier = Modifier.padding(bottom = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TechnicalMetadataHeader()
            credential.sdJwt.credentialStatus?.let {
                StatusUri(it, modifier)
                StatusIndex(it, modifier)
            }
            (credential.sdJwt.notBefore ?: credential.sdJwt.issuedAt)?.let {
                ValidFrom(it, modifier)
            }
            credential.sdJwt.expiration?.let {
                ValidUntil(it, modifier)
            }
            VcType(credential.sdJwt.verifiableCredentialType, modifier)
        }
    }
    credential.allEntries().forEach {
        LabeledText(
            label = it.first,
            text = it.second.run { slice(0..min(lastIndex, 100)) },
        )
    }
}

private fun StoreEntry.SdJwt.allEntries(): Collection<Pair<String, String>> =
    (simpleDisclosures() + complexObject()).sortedBy { it.first }.toSet()

private fun StoreEntry.SdJwt.complexObject() =
    (toComplexJson()?.entries?.map { it.key to it.value.prettyToString() } ?: listOf())

private fun StoreEntry.SdJwt.simpleDisclosures() =
    disclosures.entries.filter { it.value?.claimName != null }
        .map { it.value!!.claimName!! to it.value!!.claimValue.prettyToString() }

@Composable
private fun SingleIsoCredentialCardContent(
    credential: StoreEntry.Iso,
) {
    val modifier = Modifier.padding(bottom = 16.dp, end = 16.dp, start = 16.dp)
    ElevatedCard(modifier = Modifier.padding(bottom = 16.dp)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            TechnicalMetadataHeader()
            credential.issuerSigned.issuerAuth.payload?.status?.let {
                StatusUri(it, modifier)
                StatusIndex(it, modifier)
            }
            credential.issuerSigned.issuerAuth.payload?.validityInfo?.let {
                ValidFrom(it.validFrom, modifier)
                ValidUntil(it.validUntil, modifier)
            }
            credential.issuerSigned.issuerAuth.payload?.docType?.let {
                DocType(it, modifier)
            }
        }
    }
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

@Composable
private fun TechnicalMetadataHeader() {
    PersonAttributeDetailCardHeading(
        iconText = stringResource(Res.string.section_heading_technical_data_icon_text),
        title = stringResource(Res.string.section_heading_technical_data),
    )
}

@Composable
private fun StatusIndex(
    status: Status,
    modifier: Modifier
) {
    LabeledContent(
        label = stringResource(Res.string.text_label_status_idx),
        content = {
            Text(
                status.statusList.index.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier
    )
}

@Composable
private fun StatusUri(
    status: Status,
    modifier: Modifier
) {
    LabeledContent(
        label = stringResource(Res.string.text_label_status_uri),
        content = {
            Text(
                status.statusList.uri.string,
                softWrap = true,
            )
        },
        modifier = modifier
    )
}

@Composable
private fun ValidFrom(instant: Instant, modifier: Modifier) {
    LabeledContent(
        label = stringResource(Res.string.text_label_valid_from),
        content = {
            Text(
                instant.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun ValidUntil(instant: Instant, modifier: Modifier) {
    LabeledContent(
        label = stringResource(Res.string.text_label_valid_to),
        content = {
            Text(
                instant.toString(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun VcType(value: String, modifier: Modifier) {
    LabeledContent(
        label = stringResource(Res.string.text_label_vcType),
        content = {
            Text(
                value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

@Composable
private fun DocType(value: String, modifier: Modifier) {
    LabeledContent(
        label = stringResource(Res.string.text_label_docType),
        content = {
            Text(
                value,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        modifier = modifier,
    )
}

private fun Any.prettyToString() = when (this) {
    is Array<*> -> contentToString()
    else -> toString()
}
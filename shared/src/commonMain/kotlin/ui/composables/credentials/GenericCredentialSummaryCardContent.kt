package ui.composables.credentials

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import at.asitplus.jsonpath.core.NormalizedJsonPath
import at.asitplus.jsonpath.core.NormalizedJsonPathSegment
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.composables.LabeledText
import kotlin.math.min


@Composable
fun GenericCredentialSummaryCardContent(
    credential: SubjectCredentialStore.StoreEntry,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
    ) {
        when (credential) {
            is SubjectCredentialStore.StoreEntry.Vc -> SingleVcCredentialCardContent(
                credential = credential,
            )

            is SubjectCredentialStore.StoreEntry.SdJwt -> SingleSdJwtCredentialCardContent(
                credential = credential,
            )

            is SubjectCredentialStore.StoreEntry.Iso -> SingleIsoCredentialCardContent(
                credential = credential,
            )
        }
    }
}

@Composable
private fun ColumnScope.SingleVcCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Vc,
) {
    Text(
        credential.vc.vc.credentialSubject.toString().replace("""\[.+]""".toRegex(), "[...]")
            .replace(", ", "\n")
    )

}

@Composable
private fun ColumnScope.SingleSdJwtCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.SdJwt,
) {
    credential.disclosures.forEach {
        LabeledText(
            text = it.value?.claimValue?.toString()?.run { slice(0..min(lastIndex, 100)) } ?: "unknown claim value",
            label = it.value?.claimName ?: "unknown claim name"
        )
    }
}

@Composable
private fun ColumnScope.SingleIsoCredentialCardContent(
    credential: SubjectCredentialStore.StoreEntry.Iso,
) {
    credential.issuerSigned.namespaces?.forEach { namespace ->
        namespace.value.entries.forEach { entry ->
            LabeledText(
                text = entry.value.elementValue.prettyToString().run { slice(0..min(lastIndex, 100)) },
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
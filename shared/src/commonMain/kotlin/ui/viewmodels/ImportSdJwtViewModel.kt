package ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import at.asitplus.valera.resources.Res
import at.asitplus.valera.resources.snackbar_credential_loaded_successfully
import at.asitplus.wallet.app.common.ErrorService
import at.asitplus.wallet.app.common.SnackbarService
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import at.asitplus.wallet.lib.agent.SdJwtDecoded
import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import at.asitplus.wallet.lib.data.AttributeIndex
import at.asitplus.wallet.lib.data.SdJwtFallbackCredentialScheme
import at.asitplus.wallet.lib.data.VerifiableCredentialSdJwt
import at.asitplus.wallet.lib.jws.SdJwtSigned
import data.storage.WalletSubjectCredentialStore
import io.github.aakira.napier.Napier
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromJsonElement

class ImportSdJwtViewModel(
    private val snackbarService: SnackbarService,
    private val errorService: ErrorService,
    private val subjectCredentialStore: WalletSubjectCredentialStore,
) : ViewModel() {
    private val sdJwtImportJson = Json { ignoreUnknownKeys = true }

    fun importSdJwt(rawInput: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            runCatching {
                importSdJwtCredential(rawInput)
                snackbarService.showSnackbar(getString(Res.string.snackbar_credential_loaded_successfully))
            }.onSuccess {
                onSuccess()
            }.onFailure {
                errorService.emit(it)
            }
        }
    }

    private suspend fun importSdJwtCredential(rawInput: String) {
        val serialized = rawInput.trim()
        require(serialized.isNotEmpty()) { "Credential must not be empty" }

        val parsed = SdJwtSigned.parseCatching(serialized).onFailure {
            Napier.e("SD-JWT import: parse failed", it)
        }.getOrThrow()

        val decoded = SdJwtDecoded(parsed)
        val payload = decoded.reconstructedJsonObject
            ?: error("SD-JWT payload could not be reconstructed")

        val sdJwt = sdJwtImportJson.decodeFromJsonElement<VerifiableCredentialSdJwt>(payload)
        Napier.d("SD-JWT import: decoded vc type='${sdJwt.verifiableCredentialType}'")

        val scheme = AttributeIndex.resolveSdJwtAttributeType(sdJwt.verifiableCredentialType)
            ?: SdJwtFallbackCredentialScheme
        Napier.d("SD-JWT import: resolved scheme='${scheme.schemaUri}'")

        val store = subjectCredentialStore as? SubjectCredentialStore
            ?: error("SubjectCredentialStore is not available for direct import")

        store.storeCredential(
            vc = sdJwt,
            vcSerialized = serialized,
            disclosures = decoded.validDisclosures,
            scheme = scheme,
        )
        Napier.d("SD-JWT import: credential stored successfully")
    }
}

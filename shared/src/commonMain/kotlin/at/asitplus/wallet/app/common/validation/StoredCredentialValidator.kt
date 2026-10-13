package at.asitplus.wallet.app.common.validation

import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.validation.CredentialValidationInput
import at.asitplus.wallet.lib.validation.CredentialValidator
import at.asitplus.wallet.lib.validation.ValidationReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray

/**
 * Validates stored credentials under [ValidationPolicies.strict], e.g. to show their report, with the trust anchors and
 * the status list token resolver of the session's [credentialValidator]. The holder binding has been checked when
 * storing a credential, so it is not checked again.
 */
class StoredCredentialValidator(
    private val credentialValidator: CredentialValidator,
) {
    /** Validates [entry] off the UI dispatcher, as decoding and verifying it is expensive. */
    suspend fun validate(entry: StoreEntry): ValidationReport = withContext(Dispatchers.Default) {
        credentialValidator.validate(entry.validationInput(), ValidationPolicies.strict).report
    }
}

private fun StoreEntry.validationInput() = when (this) {
    is StoreEntry.Vc -> CredentialValidationInput.VcJwt(vcSerialized)
    is StoreEntry.SdJwt -> CredentialValidationInput.SdJwtVc(vcSerialized)
    is StoreEntry.Iso -> CredentialValidationInput.IsoMdoc(coseCompliantSerializer.encodeToByteArray(issuerSigned))
}

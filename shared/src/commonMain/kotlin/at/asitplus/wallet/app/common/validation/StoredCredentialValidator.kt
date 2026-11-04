package at.asitplus.wallet.app.common.validation

import at.asitplus.KmmResult
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.cosef.io.coseCompliantSerializer
import at.asitplus.wallet.lib.agent.SubjectCredentialStore.StoreEntry
import at.asitplus.wallet.lib.validation.CredentialValidationInput
import at.asitplus.wallet.lib.validation.CredentialValidator
import at.asitplus.wallet.lib.validation.ValidationReport
import io.github.aakira.napier.Napier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToByteArray
import ui.models.CredentialFreshnessSummaryUiModel
import ui.models.TimelinessIndicatorUiModel
import ui.models.toCredentialFreshnessSummaryModel

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

    /**
     * Whether [entry] is expired or not yet valid, and its status, see [toCredentialFreshnessSummaryModel]. It is
     * shown while selecting credentials, where an exception would end the flow, so a validation that fails reports
     * the status as not checked instead.
     */
    suspend fun freshness(entry: StoreEntry): CredentialFreshnessSummaryUiModel = catchingUnwrapped {
        validate(entry).toCredentialFreshnessSummaryModel()
    }.getOrElse {
        Napier.w("Could not check the freshness of a stored credential", it)
        CredentialFreshnessSummaryUiModel(
            timelinessIndicator = object : TimelinessIndicatorUiModel {
                override val isExpired = false
                override val isNotYetValid = false
            },
            tokenStatus = KmmResult.failure(it),
        )
    }
}

private fun StoreEntry.validationInput() = when (this) {
    is StoreEntry.Vc -> CredentialValidationInput.VcJwt(vcSerialized)
    is StoreEntry.SdJwt -> CredentialValidationInput.SdJwtVc(vcSerialized)
    is StoreEntry.Iso -> CredentialValidationInput.IsoMdoc(coseCompliantSerializer.encodeToByteArray(issuerSigned))
}

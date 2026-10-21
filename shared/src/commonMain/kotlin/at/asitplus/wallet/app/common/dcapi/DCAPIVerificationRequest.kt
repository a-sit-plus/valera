package at.asitplus.wallet.app.common.dcapi

import at.asitplus.openid.RequestParametersFrom
import kotlinx.serialization.Serializable

/**
 * A presentation request received through the DC API.
 *
 * The credentials the system selected are the [RequestParametersFrom.DcApiRequest.credentialIds] of [request].
 * [credentialQuerySelections] names the DCQL credential query each of them was selected for, as the system shows the
 * claims of that query to the user. It is empty if the system does not report the queries, e.g. for ISO mdoc requests.
 */
data class DCAPIVerificationRequest(
    val request: RequestParametersFrom.DcApiRequest,
    val credentialQuerySelections: List<DCAPICredentialQuerySelection> = emptyList(),
)

/** The system selected the credential [credentialId] to be presented for the DCQL credential query [credentialQueryId]. */
@Serializable
data class DCAPICredentialQuerySelection(
    /** The DC API identifier of the stored credential, see `SubjectCredentialStore.StoreEntry.getDcApiId` */
    val credentialId: String,
    val credentialQueryId: String,
)

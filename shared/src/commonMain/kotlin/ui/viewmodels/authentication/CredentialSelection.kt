package ui.viewmodels.authentication

import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.lib.agent.DeviceRequestCredentialDisclosure

sealed interface CredentialPresentationSubmissions<Credential: Any>

data class DCQLCredentialSubmissions<Credential: Any>(
    val credentialQuerySubmissions: Map<DCQLCredentialQueryIdentifier, List<DCQLCredentialSubmissionOption<Credential>>>?,
) : CredentialPresentationSubmissions<Credential>

data class IsoDeviceRequestCredentialSubmissions<Credential: Any>(
    val submissions: Collection<DeviceRequestCredentialDisclosure<Credential>>,
) : CredentialPresentationSubmissions<Credential>

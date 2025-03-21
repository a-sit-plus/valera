package ui.viewmodels.authentication

import at.asitplus.openid.dcql.DCQLCredentialQueryIdentifier
import at.asitplus.openid.dcql.DCQLCredentialSubmissionOption
import at.asitplus.wallet.lib.agent.PresentationExchangeCredentialDisclosure
import at.asitplus.wallet.lib.agent.SubjectCredentialStore

sealed interface CredentialPresentationSubmissions<Credential: Any>

data class DCQLCredentialSubmissions<Credential: Any>(
    val credentialQuerySubmissions: Map<DCQLCredentialQueryIdentifier, DCQLCredentialSubmissionOption<Credential>>?,
) : CredentialPresentationSubmissions<Credential>

data class PresentationExchangeCredentialSubmissions(
    val inputDescriptorSubmissions: Map<String, PresentationExchangeCredentialDisclosure>?,
) : CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>


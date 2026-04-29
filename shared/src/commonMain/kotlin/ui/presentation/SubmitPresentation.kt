package ui.presentation

import at.asitplus.wallet.lib.agent.SubjectCredentialStore
import ui.viewmodels.authentication.CredentialPresentationSubmissions

fun interface SubmitPresentation {
    operator fun invoke(
        credentialPresentationSubmissions: CredentialPresentationSubmissions<SubjectCredentialStore.StoreEntry>,
        onSuccess: (PresentationResponseRoute) -> Unit,
    )
}


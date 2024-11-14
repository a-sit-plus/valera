package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom

class PresentationService(
    val platformAdapter: PlatformAdapter,
    cryptoService: WalletCryptoService,
    holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val presentationService = PresentationServiceVck(
        openUrlExternally = { platformAdapter.openUrl(it) },
        client = httpService.buildHttpClient(),
        cryptoService = cryptoService,
        holderAgent = holderAgent,
    )

    @Throws(Throwable::class)
    suspend fun startSiop(request: AuthenticationRequestParametersFrom) {
        presentationService.startPresentation(request)
    }

}
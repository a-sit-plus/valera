package at.asitplus.wallet.app.common

import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import io.github.aakira.napier.Napier

class PresentationService(
    val platformAdapter: PlatformAdapter,
    cryptoService: WalletCryptoService,
    holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val presentationService = PresentationServiceVck(
        platformAdapter = platformAdapter,
        cryptoService = cryptoService,
        holderAgent = holderAgent,
        httpService = httpService,
    )

    @Throws(Throwable::class)
    suspend fun startSiop(
        authenticationRequestParameters: AuthenticationRequestParametersFrom,
    ) {
        presentationService.startSiop(authenticationRequestParameters)
    }

}
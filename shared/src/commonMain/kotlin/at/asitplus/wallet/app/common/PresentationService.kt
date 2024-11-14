package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
import at.asitplus.wallet.lib.oidc.AuthenticationRequestParametersFrom
import at.asitplus.wallet.lib.oidc.helpers.AuthorizationResponsePreparationState
import io.ktor.client.HttpClient

class PresentationService(
    val platformAdapter: PlatformAdapter,
    cryptoService: WalletCryptoService,
    holderAgent: HolderAgent,
    httpService: HttpService,
) {
    private val presentationService = OpenId4VpWallet(
        openUrlExternally = { platformAdapter.openUrl(it) },
        engine = HttpClient().engine,
        httpClientConfig = httpService.loggingConfig,
        cryptoService = cryptoService,
        holderAgent = holderAgent,
    )

    @Throws(Throwable::class)
    suspend fun startSiop(request: AuthenticationRequestParametersFrom) {
        presentationService.startPresentation(request)
    }

    suspend fun parseAuthenticationRequestParameters(requestUri: String): KmmResult<AuthenticationRequestParametersFrom> {
        return presentationService.oidcSiopWallet.parseAuthenticationRequestParameters(requestUri)
    }

    suspend fun startAuthorizationResponsePreparation(request: AuthenticationRequestParametersFrom): KmmResult<AuthorizationResponsePreparationState> {
        return presentationService.oidcSiopWallet.startAuthorizationResponsePreparation(request)
    }

}
package at.asitplus.wallet.app.common

import at.asitplus.KmmResult
import at.asitplus.openid.AuthenticationRequestParameters
import at.asitplus.openid.RequestParametersFrom
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.ktor.openid.OpenId4VpWallet
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
    suspend fun startSiop(request: RequestParametersFrom<AuthenticationRequestParameters>) {
        presentationService.startPresentation(request)
    }

    suspend fun parseAuthenticationRequestParameters(requestUri: String): KmmResult<RequestParametersFrom<AuthenticationRequestParameters>> {
        return presentationService.oidcSiopWallet.parseAuthenticationRequestParameters(requestUri)
    }

    suspend fun startAuthorizationResponsePreparation(
        request: RequestParametersFrom<AuthenticationRequestParameters>
    ): KmmResult<AuthorizationResponsePreparationState> {
        return presentationService.oidcSiopWallet.startAuthorizationResponsePreparation(request)
    }

}
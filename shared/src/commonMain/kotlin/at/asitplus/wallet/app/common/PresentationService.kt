package at.asitplus.wallet.app.common

import DataStoreService
import at.asitplus.wallet.lib.agent.CryptoService
import at.asitplus.wallet.lib.agent.HolderAgent
import at.asitplus.wallet.lib.oidc.OidcSiopWallet
import io.github.aakira.napier.Napier

class PresentationService(val platformAdapter: PlatformAdapter, val dataStoreService: DataStoreService, val cryptoService: CryptoService, val holderAgent: HolderAgent) {
    suspend fun startSiop(url: String){
        Napier.d("Start SIOP process")
        val oidcSiopWallet = OidcSiopWallet.newInstance(
            holder = holderAgent,
            cryptoService = cryptoService,
        )
        val authenticationResponse = oidcSiopWallet.createAuthnResponse(url)
        if (authenticationResponse.isFailure) {
            throw Exception("Failure in received authentication response")
        }
        Napier.d("Opening $authenticationResponse")
        when (val response= authenticationResponse.getOrThrow()){
            is OidcSiopWallet.AuthenticationResponseResult.Post -> {
                TODO("Function not implemented")
            }
            is OidcSiopWallet.AuthenticationResponseResult.Redirect -> platformAdapter.openUrl(response.url)
        }


    }
}
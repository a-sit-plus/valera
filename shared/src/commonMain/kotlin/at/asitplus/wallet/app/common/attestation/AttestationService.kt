package at.asitplus.wallet.app.common.attestation

import at.asitplus.catching
import at.asitplus.openid.ClientNonceResponse
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.KeyAttestationJwt
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.ktor.openid.OAuth2KtorClient.LoadInstanceAttestationInput
import at.asitplus.wallet.lib.oidvci.WalletService.KeyAttestationInput
import io.github.aakira.napier.Napier
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.appendEncodedPathSegments
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class AttestationService(
    holderKey: WalletKeyMaterial,
    private val config: SettingsRepository,
    buildContext: BuildContext,
    httpService: HttpService
) {
    val httpClient = httpService.buildHttpClient()

    val challengeEndpoint = config.walletProviderHost.map {
        URLBuilder(it).apply {
            appendEncodedPathSegments(PATH_NONCE)
        }
    }

    val instanceAttestationHelper = InstanceAttestationHelper(
        config.walletProviderHost,
        httpClient,
        buildContext
    )
    
    val keyAttestationHelper = KeyAttestationHelper(config.walletProviderHost, httpClient, holderKey)
    val bufferedInstanceAttestation = MutableStateFlow<JwsSigned<JsonWebToken>?>(null)
    val bufferedKeyAttestation = MutableStateFlow<JwsSigned<KeyAttestationJwt>?>(null)

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun preloadInstanceAttestation() = catching {
        Napier.d("AttestationService: Preload instance attestation")
        requestInstanceAttestation().let {
            bufferedInstanceAttestation.emit(it)
        }
    }.onFailure {
        Napier.e("AttestationService: Error preloading instance attestation. $it")
    }
    suspend fun preloadKeyAttestation() = catching {
        Napier.d("AttestationService: Preload key attestation")
        requestKeyAttestation(
            KeyAttestationInput(
                clientNonce = null,
                credentialIssuer = null,
                supportedAlgorithms = null,
                preferredKeyStorageStatusPeriod = PREFERRED_DEFAULT_TTL,
            )
        ).let {
            bufferedKeyAttestation.emit(it)
        }
    }.onFailure {
        Napier.e("AttestationService: Error preloading key attestation. $it")
    }

    suspend fun loadInstanceAttestation(input: LoadInstanceAttestationInput) = catching {
        requestInstanceAttestation(input.preferredClientStatusPeriod ?: PREFERRED_DEFAULT_TTL).let { instanceAttestation ->
            bufferedInstanceAttestation.emit(null)
            instanceAttestation
        }
    }

    suspend fun loadKeyAttestation(input: KeyAttestationInput) = catching {
        requestKeyAttestation(input).also { keyAttestation ->
            bufferedKeyAttestation.emit(keyAttestation)
        }
    }

    fun getWalletProviderHost() = config.walletProviderHost
    fun setWalletProviderHost(host: String) = scope.launch {
        config.set(walletProviderHost = host)
        bufferedKeyAttestation.emit(null)
        bufferedInstanceAttestation.emit(null)
    }

    private suspend fun requestInstanceAttestation(
        preferredClientStatusPeriod: Duration = PREFERRED_DEFAULT_TTL
    ): JwsSigned<JsonWebToken> {
        bufferedInstanceAttestation.firstOrNull()?.let { buffer ->
            if (buffer.hasRemainingClientStatusPeriod(preferredClientStatusPeriod)) {
                Napier.d("AttestationService: Use buffered instance attestation")
                return buffer
            }
        }

        Napier.d("AttestationService: Request new instance attestation")
        val instanceAttestation = instanceAttestationHelper.requestInstanceAttestation(preferredClientStatusPeriod)
        bufferedInstanceAttestation.emit(instanceAttestation)
        return instanceAttestation
    }

    private suspend fun requestKeyAttestation(
        input: KeyAttestationInput,
    ): JwsSigned<KeyAttestationJwt> {
        val preferredKeyStorageStatusPeriod = input.preferredKeyStorageStatusPeriod ?: PREFERRED_DEFAULT_TTL

        bufferedKeyAttestation.firstOrNull()?.let { buffer ->
            if (buffer.hasRemainingKeyStorageStatusPeriod(preferredKeyStorageStatusPeriod)) {
                Napier.d("AttestationService: Use buffered key attestation")
                bufferedKeyAttestation.emit(null)
                return buffer
            }
        }

        Napier.d("AttestationService: Request new key attestation")

        val instanceAttestation = requestInstanceAttestation()

        val pop = instanceAttestationHelper.buildProofOfPossession(
            audience = config.walletProviderHost.first(),
            nonce = getChallenge()
        )

        return keyAttestationHelper.requestKeyAttestation(
            instanceAttestation = instanceAttestation,
            pop = pop,
            nonce = input.clientNonce,
            preferredKeyStorageStatusPeriod = input.preferredKeyStorageStatusPeriod,
            supportedAlgorithms = input.supportedAlgorithms
        )
    }

    private fun JwsSigned<KeyAttestationJwt>.hasRemainingKeyStorageStatusPeriod(ttl: Duration): Boolean {
        val now = Clock.System.now()
        return payload.expiration?.let { it > now + 10.seconds } == true &&
                payload.keyStorageStatus?.expiration?.let { it > now + ttl } == true
    }

    private fun JwsSigned<JsonWebToken>.hasRemainingClientStatusPeriod(ttl: Duration): Boolean {
        val now = Clock.System.now()
        return payload.expiration?.let { it > now + 10.seconds } == true &&
                payload.clientStatus?.expiration?.let { it > now + ttl } == true
    }

    private suspend fun getChallenge() = catching {
        httpClient.get(challengeEndpoint.first().build()) {
        }.body<ClientNonceResponse>().clientNonce
    }.onFailure {
        Napier.e("AttestationService: Error receiving challenge. $it")
    }.getOrNull()

}
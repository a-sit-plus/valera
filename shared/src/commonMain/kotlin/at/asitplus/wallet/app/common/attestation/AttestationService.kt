package at.asitplus.wallet.app.common.attestation

import at.asitplus.catching
import at.asitplus.catchingUnwrapped
import at.asitplus.openid.ClientNonceResponse
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsCompactTyped
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
import kotlin.time.Duration.Companion.seconds

class AttestationService(
    holderKey: WalletKeyMaterial,
    private val config: SettingsRepository,
    buildContext: BuildContext,
    httpService: HttpService,
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    private val httpClient = httpService.buildHttpClient()

    private fun challengeEndpoint() = config.walletProviderHost.map {
        URLBuilder(it).apply {
            appendEncodedPathSegments(PATH_NONCE)
        }
    }

    private val instanceAttestationHelper = InstanceAttestationHelper(
        config,
        httpClient,
        buildContext,
    )

    private val keyAttestationHelper = KeyAttestationHelper(config, httpClient, holderKey)
    val bufferedInstanceAttestation = MutableStateFlow<JwsCompactTyped<JsonWebToken>?>(null)
    val bufferedKeyAttestation = MutableStateFlow<JwsCompactTyped<KeyAttestationJwt>?>(null)

    suspend fun reset() {
        bufferedKeyAttestation.emit(null)
        bufferedInstanceAttestation.emit(null)
        instanceAttestationHelper.reset()
    }

    suspend fun getInstanceAttestationKeyMaterial() = instanceAttestationHelper.keyMaterial()

    suspend fun preloadInstanceAttestation() = catchingUnwrapped {
        Napier.d("AttestationService: Preload instance attestation")
        requestInstanceAttestation(preloadInstanceAttestationInput).let {
            bufferedInstanceAttestation.emit(it)
        }
    }.onFailure {
        Napier.e("AttestationService: Error preloading instance attestation. $it")
    }

    suspend fun preloadKeyAttestation() = catchingUnwrapped {
        Napier.d("AttestationService: Preload key attestation")
        requestKeyAttestation(preloadKeyAttestationInput).let {
            bufferedKeyAttestation.emit(it)
        }
    }.onFailure {
        Napier.e("AttestationService: Error preloading key attestation. $it")
    }

    suspend fun loadInstanceAttestation(input: LoadInstanceAttestationInput) = catching {
        requestInstanceAttestation(input)
    }

    suspend fun loadKeyAttestation(input: KeyAttestationInput) = catching {
        requestKeyAttestation(input)
    }

    fun getWalletProviderHost() = config.walletProviderHost
    fun setWalletProviderHost(host: String) = scope.launch {
        config.set(walletProviderHost = host)
        reset()
    }

    private suspend fun requestInstanceAttestation(
        input: LoadInstanceAttestationInput,
    ): JwsCompactTyped<JsonWebToken> {
        if (input.allowBuffer()) {
            bufferedInstanceAttestation.firstOrNull()?.let { buffer ->
                if (buffer.hasRemainingClientStatusPeriod(input.preferredClientStatusPeriod ?: PREFERRED_DEFAULT_TTL)) {
                    Napier.d("AttestationService: Use buffered instance attestation")
                    return buffer
                }
            }
        }


        Napier.d("AttestationService: Request new instance attestation")
        return instanceAttestationHelper.instanceAttestation(input.preferredClientStatusPeriod ?: PREFERRED_DEFAULT_TTL)
            .also {
                if (input.allowBuffer()) {
                    bufferedInstanceAttestation.emit(it)
                }
            }
    }

    private suspend fun requestKeyAttestation(
        input: KeyAttestationInput,
    ): JwsCompactTyped<KeyAttestationJwt> {
        if (input.allowBuffer()) {
            bufferedKeyAttestation.firstOrNull()?.let { buffer ->
                if (buffer.hasRemainingKeyStorageStatusPeriod(input.preferredKeyStorageStatusPeriod ?: PREFERRED_DEFAULT_TTL)) {
                    Napier.d("AttestationService: Use buffered key attestation")
                    bufferedKeyAttestation.emit(null)
                    return buffer
                }
            }
        }

        Napier.d("AttestationService: Request new key attestation")

        val instanceAttestation = requestInstanceAttestation(preloadInstanceAttestationInput)

        val pop = instanceAttestationHelper.buildProofOfPossession(
            audience = config.walletProviderHost.first(), nonce = getChallenge()
        )

        return keyAttestationHelper.requestKeyAttestation(
            instanceAttestation = instanceAttestation,
            pop = pop,
            nonce = input.clientNonce,
            preferredKeyStorageStatusPeriod = input.preferredKeyStorageStatusPeriod,
            supportedAlgorithms = input.supportedAlgorithms
        ).also {
            if (input.allowBuffer()) {
                bufferedKeyAttestation.emit(it)
            }
        }
    }

    private fun JwsCompactTyped<KeyAttestationJwt>.hasRemainingKeyStorageStatusPeriod(ttl: Duration): Boolean {
        val now = Clock.System.now()
        return payload.expiration?.let { it > now + 10.seconds } == true && payload.keyStorageStatus?.expiration?.let { it > now + ttl } == true
    }

    private fun JwsCompactTyped<JsonWebToken>.hasRemainingClientStatusPeriod(ttl: Duration): Boolean {
        val now = Clock.System.now()
        return payload.expiration?.let { it > now + 10.seconds } == true && payload.clientStatus?.expiration?.let { it > now + ttl } == true
    }

    private suspend fun getChallenge() = catchingUnwrapped {
        httpClient.get(challengeEndpoint().first().build()) {}.body<ClientNonceResponse>().clientNonce
    }.onFailure {
        Napier.e("AttestationService: Error receiving challenge. $it")
    }.getOrNull()

}

fun KeyAttestationInput.allowBuffer() = (this.credentialIssuer == null && this.clientNonce == null)
fun LoadInstanceAttestationInput.allowBuffer() = false

val preloadInstanceAttestationInput = LoadInstanceAttestationInput("", PREFERRED_DEFAULT_TTL)
val preloadKeyAttestationInput = KeyAttestationInput(null, null, null, PREFERRED_DEFAULT_TTL)
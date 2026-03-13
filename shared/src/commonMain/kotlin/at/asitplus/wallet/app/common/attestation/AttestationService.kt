package at.asitplus.wallet.app.common.attestation

import at.asitplus.catching
import at.asitplus.signum.indispensable.josef.JsonWebToken
import at.asitplus.signum.indispensable.josef.JwsSigned
import at.asitplus.signum.indispensable.josef.KeyAttestationJwt
import at.asitplus.signum.indispensable.pki.X509Certificate
import at.asitplus.signum.supreme.sign.Signer
import at.asitplus.wallet.app.common.BuildContext
import at.asitplus.wallet.app.common.HttpService
import at.asitplus.wallet.app.common.WalletKeyMaterial
import at.asitplus.wallet.app.common.data.SettingsRepository
import at.asitplus.wallet.lib.agent.SignerBasedKeyMaterial
import at.asitplus.wallet.lib.oidvci.WalletService.LoadUnitAttestationPopInput
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds

class AttestationService(
    val keyMaterial: WalletKeyMaterial,
    private val config: SettingsRepository,
    private val buildContext: BuildContext,
    httpService: HttpService
) {
    val instanceAttestationHelper = InstanceAttestationHelper(config.walletProviderHost, httpService)
    val unitAttestationHelper = UnitAttestationHelper(config.walletProviderHost, httpService, keyMaterial)
    var bufferedInstanceAttestation = MutableStateFlow<JwsSigned<JsonWebToken>?>(null)
    var bufferedUnitAttestation = MutableStateFlow<JwsSigned<KeyAttestationJwt>?>(null)

    private val scope = CoroutineScope(Dispatchers.IO)

    suspend fun preloadAttestation() = catching {
        Napier.d("AttestationService: Preload attestation")
        requestInstanceAttestation().let {
            bufferedInstanceAttestation.emit(it)
        }
        requestUnitAttestation().let {
            bufferedUnitAttestation.emit(it)
        }
    }

    suspend fun loadInstanceAttestation() = catching {
        requestInstanceAttestation().let { instanceAttestation ->
            bufferedInstanceAttestation.emit(null)
            instanceAttestation
        }
    }


    suspend fun loadInstanceAttestationPop(nonce: String? = null) = catching {
        instanceAttestationHelper.buildProofOfPossession(nonce)
    }

    suspend fun loadUnitAttestationPop(input: LoadUnitAttestationPopInput) = catching {
        requestUnitAttestation(input.ttl).let { unitAttestation ->
            unitAttestationHelper.buildProofOfPossession(unitAttestation, input.type, input.payload).also {
                bufferedUnitAttestation.emit(unitAttestation)
            }
        }
    }


    fun getWalletProviderHost() = config.walletProviderHost
    fun setWalletProviderHost(host: String) = scope.launch {
        config.set(walletProviderHost = host)
        bufferedUnitAttestation.emit(null)
        bufferedInstanceAttestation.emit(null)
    }

    private suspend fun requestInstanceAttestation(): JwsSigned<JsonWebToken> {
        bufferedInstanceAttestation.firstOrNull()?.let { buffer ->
            if ((buffer.payload.expiration)?.let { it > Clock.System.now() + 10.seconds } == true) {
                Napier.d("AttestationService: Use buffered instance attestation")
                return buffer
            }
        }

        Napier.d("AttestationService: Request new instance attestation")
        val instanceAttestation = instanceAttestationHelper.requestInstanceAttestation(buildContext.versionName)
        bufferedInstanceAttestation.emit(instanceAttestation)
        return instanceAttestation
    }

    private suspend fun requestUnitAttestation(
        ttl: Duration = 31.days
    ): JwsSigned<KeyAttestationJwt> {
        bufferedUnitAttestation.firstOrNull()?.let { buffer ->
            if ((buffer.payload.expiration)?.let { it > Clock.System.now() + ttl } == true) {
                Napier.d("AttestationService: Use buffered unit attestation")
                return buffer
            }
        }

        Napier.d("AttestationService: Request new unit attestation")

        val instanceAttestation = requestInstanceAttestation()
        val pop = instanceAttestationHelper.buildProofOfPossession()

        return unitAttestationHelper.requestUnitAttestation(instanceAttestation, pop)
    }
}

class HolderKeyMaterial(
    signer: Signer,
) : SignerBasedKeyMaterial(signer) {
    override suspend fun getCertificate(): X509Certificate? = null
}
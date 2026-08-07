package at.asitplus.wallet.app.common

import at.asitplus.authcheckkit.AuthCheckKit
import at.asitplus.authcheckkit.BiometricSecurityClass
import at.asitplus.catchingUnwrapped
import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.app.common.Configuration.DATASTORE_CAPABILITIES_ATTESTATION
import data.storage.DataStoreService
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import io.ktor.network.tls.TlsException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import ui.navigation.routes.RoutePrerequisites

interface CapabilitiesService {
    fun getDeviceStatus(): Flow<CapabilitiesData?>
    suspend fun refreshStatus()
    suspend fun reset()
    fun evaluatePrerequisites(list: Set<RoutePrerequisites>): Flow<Boolean>
}

class RealCapabilitiesService(
    private val keyStoreService: KeystoreService,
    private val dataStoreService: DataStoreService,
    private val platformAdapter: PlatformAdapter,
    private val httpService: HttpService,
    sessionCoroutineScope: CoroutineScope,
) : CapabilitiesService {
    private val httpClient = httpService.buildHttpClient()
    private val refreshTrigger = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val deviceLockStatus: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .map { AuthCheckKit.getDeviceStatus().isDeviceLockSet }
    val biometryEnrolledStatus: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .map { (AuthCheckKit.getDeviceStatus().biometryEnrollment == BiometricSecurityClass.CLASS3) }
    val onlineStatus: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .map { getOnlineStatus() }
        .shareIn(sessionCoroutineScope, SharingStarted.Eagerly, replay = 1)
    val attestationStatus: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .map { getAttestationStatus() }

    val signerStatus: Flow<Boolean> = refreshTrigger
        .onStart { emit(Unit) }
        .map { getSignerStatus() }

    val cameraPermission: Flow<Boolean?> = refreshTrigger
        .onStart { emit(Unit) }
        .map { platformAdapter.getCameraPermission() }

    val flows = listOf(
        deviceLockStatus,
        biometryEnrolledStatus,
        onlineStatus,
        attestationStatus,
        signerStatus,
        cameraPermission
    )

    override fun getDeviceStatus(): Flow<CapabilitiesData?> =
        combine(flows) {
            catchingUnwrapped {
                CapabilitiesData(
                    it[0] as Boolean,
                    it[1] as Boolean,
                    it[2] as Boolean,
                    it[3] as Boolean,
                    it[4] as Boolean,
                    it[5]

                )
            }.getOrNull()
        }

    init {
        sessionCoroutineScope.coroutineContext[Job]?.invokeOnCompletion { httpClient.close() }
    }

    override suspend fun refreshStatus() {
        refreshTrigger.emit(Unit)
    }

    private suspend fun getOnlineStatus(): Boolean = catchingUnwrapped {
        httpClient.get("https://wallet.a-sit.plus/check.json").bodyAsBytes()
    }.fold(
        onSuccess = { true },
        onFailure = { it.isTlsError() },
    )

    private suspend fun getSignerStatus() = keyStoreService.testSigner()

    private suspend fun getAttestationStatus(): Boolean =
        getAttestationPreference().onSuccess {
            return true
        }.onFailure {
            return keyStoreService.testAttestation().also { setAttestationPreference(it) }
        }.getOrElse {
            return false
        }


    private suspend fun getAttestationPreference() = catchingUnwrapped {
        joseCompliantSerializer.decodeFromString<Boolean>(
            dataStoreService.getPreference(DATASTORE_CAPABILITIES_ATTESTATION).first()!!
        )
    }


    private suspend fun setAttestationPreference(value: Boolean) =
        dataStoreService.setPreference(
            key = DATASTORE_CAPABILITIES_ATTESTATION, value = joseCompliantSerializer.encodeToString(value)
        )


    override suspend fun reset() =
        dataStoreService.deletePreference(DATASTORE_CAPABILITIES_ATTESTATION)


    private val stateMap: Map<RoutePrerequisites, List<Flow<Boolean?>>> = mapOf(
        RoutePrerequisites.CRYPTO to listOf(deviceLockStatus, biometryEnrolledStatus, attestationStatus),
        RoutePrerequisites.INTERNET to listOf(onlineStatus),
        RoutePrerequisites.CAMERA to listOf(cameraPermission)
    )

    override fun evaluatePrerequisites(list: Set<RoutePrerequisites>): Flow<Boolean> {
        val flows = list.flatMap { stateMap[it].orEmpty() }
        return if (flows.isEmpty()) {
            flowOf(false)
        } else {
            combine(flows) { values ->
                values.all { it == true }
            }
        }
    }
}

internal fun Throwable.isTlsError(): Boolean =
    generateSequence(this) { it.cause }.any { it is TlsException || it.isPlatformTlsError() }

internal expect fun Throwable.isPlatformTlsError(): Boolean

data class CapabilitiesData(
    val deviceLockStatus: Boolean,
    val biometryEnrolledStatus: Boolean,
    val onlineStatus: Boolean,
    val attestationStatus: Boolean,
    val signingStatus: Boolean,
    val cameraPermission: Boolean?
)

package at.asitplus.wallet.app.common

import at.asitplus.authcheckkit.AuthCheckKit
import at.asitplus.authcheckkit.BiometricSecurityClass
import at.asitplus.catchingUnwrapped
import at.asitplus.wallet.app.common.Configuration.DATASTORE_CAPABILITIES_ATTESTATION
import at.asitplus.wallet.lib.data.vckJsonSerializer
import data.storage.DataStoreService
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
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
    private val platformAdapter: PlatformAdapter
) : CapabilitiesService {
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
            runCatching {
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
        CoroutineScope(Dispatchers.IO).launch {
            refreshStatus()
        }
    }

    override suspend fun refreshStatus() {
        refreshTrigger.emit(Unit)
    }

    private suspend fun getOnlineStatus() = catchingUnwrapped {
        HttpClient().get("https://wallet.a-sit.at/check.json")
    }.isSuccess

    private suspend fun getSignerStatus() = keyStoreService.testSigner()

    private suspend fun getAttestationStatus() =
        getAttestationPreference().onSuccess {
            return true
        }.onFailure {
            return keyStoreService.testAttestation().also { setAttestationPreference(it) }
        }.getOrElse {
            return false
        }


    private suspend fun getAttestationPreference() =
        runCatching {
            vckJsonSerializer.decodeFromString<Boolean>(
                dataStoreService.getPreference(DATASTORE_CAPABILITIES_ATTESTATION).first()!!
            )
        }


    private suspend fun setAttestationPreference(value: Boolean) =
        dataStoreService.setPreference(
            key = DATASTORE_CAPABILITIES_ATTESTATION, value = vckJsonSerializer.encodeToString(value)
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

data class CapabilitiesData(
    val deviceLockStatus: Boolean,
    val biometryEnrolledStatus: Boolean,
    val onlineStatus: Boolean,
    val attestationStatus: Boolean,
    val signingStatus: Boolean,
    val cameraPermission: Boolean?
)
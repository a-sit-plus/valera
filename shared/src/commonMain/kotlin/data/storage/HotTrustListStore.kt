package data.storage

import at.asitplus.etsi.ListOfTrustedEntities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

class HotTrustListStore(
    private val delegate: PersistentTrustListStore,
    val coroutineScope: CoroutineScope
) {
    val hotTrustContainer: StateFlow<Map<String, ListOfTrustedEntities>?> =
        delegate.observeTrustContainer().stateIn(
            scope = coroutineScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    fun observeTrustContainer(): Flow<Map<String, ListOfTrustedEntities>> =
        hotTrustContainer.filterNotNull()

    suspend fun getActiveCache(): Map<String, ListOfTrustedEntities> {
        return observeTrustContainer().first()
    }
}
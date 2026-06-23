package data.storage

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.utils.MapStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

class PersistentMapStore<Key : Any, Value : Any>(
    private val dataStoreService: DataStoreService,
    private val preferenceKey: String,
    private val keySerializer: KSerializer<Key>,
    private val valueSerializer: KSerializer<Value>,
    private val lifetime: Duration = 10.minutes,
    private val clock: Clock = Clock.System,
) : MapStore<Key, Value> {

    init {
        require(lifetime > Duration.ZERO) { "lifetime must be > 0" }
    }

    private val mutex = Mutex()
    private val serializer = MapSerializer(
        keySerializer = keySerializer,
        valueSerializer = PersistentMapStoreEntry.serializer(valueSerializer),
    )

    override suspend fun put(key: Key, value: Value) = mutex.withLock {
        val entries = loadEntriesLocked()
        cleanupExpiredLocked(entries)
        entries[key] = PersistentMapStoreEntry(
            value = value,
            expirationEpochMilliseconds = (clock.now() + lifetime).toEpochMilliseconds(),
        )
        saveEntriesLocked(entries)
    }

    override suspend fun get(key: Key): Value? = mutex.withLock {
        val entries = loadEntriesLocked()
        val didCleanup = cleanupExpiredLocked(entries)
        val entry = entries[key]
        if (didCleanup) {
            saveEntriesLocked(entries)
        }
        entry?.value
    }

    override suspend fun remove(key: Key): Value? = mutex.withLock {
        val entries = loadEntriesLocked()
        cleanupExpiredLocked(entries)
        val removed = entries.remove(key)
        saveEntriesLocked(entries)
        removed?.value
    }

    private suspend fun loadEntriesLocked(): MutableMap<Key, PersistentMapStoreEntry<Value>> =
        dataStoreService.getPreference(preferenceKey).firstOrNull()
            ?.let { serialized ->
                joseCompliantSerializer.decodeFromString(serializer, serialized).toMutableMap()
            } ?: mutableMapOf()

    private suspend fun saveEntriesLocked(entries: Map<Key, PersistentMapStoreEntry<Value>>) {
        if (entries.isEmpty()) {
            dataStoreService.deletePreference(preferenceKey)
        } else {
            dataStoreService.setPreference(
                key = preferenceKey,
                value = joseCompliantSerializer.encodeToString(serializer, entries),
            )
        }
    }

    private fun cleanupExpiredLocked(entries: MutableMap<Key, PersistentMapStoreEntry<Value>>): Boolean {
        val now = clock.now().toEpochMilliseconds()
        return entries.entries.removeAll { it.value.expirationEpochMilliseconds < now }
    }
}

@Serializable
private data class PersistentMapStoreEntry<Value>(
    val value: Value,
    val expirationEpochMilliseconds: Long,
)

fun persistentStringMapStore(
    dataStoreService: DataStoreService,
    preferenceKey: String,
    lifetime: Duration = 10.minutes,
    clock: Clock = Clock.System,
): PersistentMapStore<String, String> = PersistentMapStore(
    dataStoreService = dataStoreService,
    preferenceKey = preferenceKey,
    keySerializer = String.serializer(),
    valueSerializer = String.serializer(),
    lifetime = lifetime,
    clock = clock,
)

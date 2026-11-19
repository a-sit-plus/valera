package data.storage

import at.asitplus.signum.indispensable.josef.io.joseCompliantSerializer
import at.asitplus.wallet.lib.utils.MapStore
import kotlinx.coroutines.flow.firstOrNull
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

    private val serializer = MapSerializer(
        keySerializer = keySerializer,
        valueSerializer = PersistentMapStoreEntry.serializer(valueSerializer),
    )

    override suspend fun put(key: Key, value: Value) = editEntries { entries ->
        entries[key] = expiringEntry(value)
    }

    override suspend fun get(key: Key): Value? {
        val entries = decodeEntries(dataStoreService.getPreference(preferenceKey).firstOrNull())
        if (cleanupExpired(entries)) editEntries { }
        return entries[key]?.value
    }

    override suspend fun remove(key: Key): Value? {
        var removed: Value? = null
        editEntries { removed = it.remove(key)?.value }
        return removed
    }

    /** Changes an entry only if its current, unexpired value matches [expected]. */
    internal suspend fun compareAndSet(key: Key, expected: Value?, value: Value?): Boolean {
        var exchanged = false
        editEntries { entries ->
            exchanged = entries[key]?.value == expected
            if (exchanged) {
                if (value == null) entries.remove(key)
                else entries[key] = expiringEntry(value)
            }
        }
        return exchanged
    }

    private fun expiringEntry(value: Value) =
        PersistentMapStoreEntry(value, (clock.now() + lifetime).toEpochMilliseconds())

    private fun decodeEntries(serialized: String?): MutableMap<Key, PersistentMapStoreEntry<Value>> =
        serialized?.let { joseCompliantSerializer.decodeFromString(serializer, it).toMutableMap() } ?: mutableMapOf()

    private suspend fun editEntries(edit: (MutableMap<Key, PersistentMapStoreEntry<Value>>) -> Unit) {
        dataStoreService.updatePreference(preferenceKey) { serialized ->
            val entries = decodeEntries(serialized)
            cleanupExpired(entries)
            edit(entries)
            entries.takeIf { it.isNotEmpty() }?.let { joseCompliantSerializer.encodeToString(serializer, it) }
        }
    }

    private fun cleanupExpired(entries: MutableMap<Key, PersistentMapStoreEntry<Value>>): Boolean {
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

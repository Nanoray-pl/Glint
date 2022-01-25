package pl.nanoray.glint.store.map

import pl.nanoray.glint.store.MutableStore
import pl.nanoray.glint.store.Store

interface MapStore<Key, Value> {
    val keys: Set<Key>

    operator fun get(key: Key): Value
}

interface MutableMapStore<Key, Value>: MapStore<Key, Value?> {
    operator fun set(key: Key, value: Value)
    fun remove(key: Key)
}

interface NonNullMutableMapStore<Key, Value>: MapStore<Key, Value> {
    operator fun set(key: Key, value: Value)
    fun remove(key: Key)
}

class MapStoreWrapper<Key, Value>(
    private val wrapped: Store<Map<Key, Value>>
): MapStore<Key, Value?> {
    override val keys: Set<Key>
        get() = wrapped.value.keys

    override fun get(key: Key): Value? {
        return wrapped.value[key]
    }
}

class MutableMapStoreWrapper<Key, Value>(
    private val wrapped: MutableStore<Map<Key, Value>>
): MutableMapStore<Key, Value> {
    override val keys: Set<Key>
        get() = wrapped.value.keys

    override fun get(key: Key): Value? {
        return wrapped.value[key]
    }

    override fun set(key: Key, value: Value) {
        wrapped.value = wrapped.value.toMutableMap().apply { this[key] = value }
    }

    override fun remove(key: Key) {
        wrapped.value = wrapped.value.toMutableMap().apply { remove(key) }
    }
}

fun <Key, Value> Store<Map<Key, Value>>.toMapStore(): MapStore<Key, Value?> {
    return MapStoreWrapper(this)
}

fun <Key, Value> MutableStore<Map<Key, Value>>.toMutableMapStore(): MutableMapStore<Key, Value> {
    return MutableMapStoreWrapper(this)
}

class MapStoreValueStore<Key, Value>(
    private val wrapped: MapStore<Key, Value>,
    private val key: Key
): Store<Value> {
    override val value: Value
        get() = wrapped[key]
}

class MapStoreMutableValueStore<Key, Value>(
    private val wrapped: MutableMapStore<Key, Value>,
    private val key: Key
): MutableStore<Value?> {
    override var value: Value?
        get() = wrapped[key]
        set(value) {
            if (value == null)
                wrapped.remove(key)
            else
                wrapped[key] = value
        }
}

fun <Key, Value> MapStore<Key, Value>.toValueStoreForKey(key: Key): Store<Value> {
    return MapStoreValueStore(this, key)
}

fun <Key, Value> MutableMapStore<Key, Value>.toMutableValueStoreForKey(key: Key): MutableStore<Value?> {
    return MapStoreMutableValueStore(this, key)
}
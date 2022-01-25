package pl.nanoray.glint.store.map

import pl.nanoray.glint.store.Store

class DefaultFromNonMapStoreMapStore<Key, Value>(
    private val contextStore: MapStore<Key, Value>,
    private val globalStore: Store<Value>
): MapStore<Key, Value> {
    override val keys: Set<Key>
        get() = contextStore.keys

    override fun get(key: Key): Value {
        return contextStore[key] ?: globalStore.value
    }
}

fun <Key, Value> MapStore<Key, Value>.replacingNull(globalStore: Store<Value>): DefaultFromNonMapStoreMapStore<Key, Value> {
    return DefaultFromNonMapStoreMapStore(this, globalStore)
}
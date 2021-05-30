package pl.nanoray.glint.store

class ValueFromMapStore<Key, Value>(
	private val map: MutableMap<Key, Value>,
	private val key: Key
): Store<Value?> {
	override var value: Value?
		get() = map[key]
		set(value) {
			if (value == null)
				map.remove(key)
			else
				map[key] = value
		}
}

class ValueFromMapStoreStore<Key, Value>(
	private val store: Store<Map<Key, Value>>,
	private val key: Key
): Store<Value?> {
	override var value: Value?
		get() = store.value[key]
		set(value) {
			val result = store.value.toMutableMap()
			if (value == null)
				result.remove(key)
			else
				result[key] = value
			store.value = result
		}
}

fun <Key, Value> Store<Map<Key, Value>>.forKey(key: Key): ValueFromMapStoreStore<Key, Value> {
	return ValueFromMapStoreStore(this, key)
}
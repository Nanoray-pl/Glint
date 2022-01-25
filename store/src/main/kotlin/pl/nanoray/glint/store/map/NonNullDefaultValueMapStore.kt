package pl.nanoray.glint.store.map

class NonNullDefaultValueMapStore<Key, Value: Any>(
    private val wrapped: MapStore<Key, Value?>,
    private val defaultValueSupplier: (Key) -> Value
): MapStore<Key, Value> {
    override val keys: Set<Key>
        get() = wrapped.keys

    constructor(
        wrapped: MapStore<Key, Value?>,
        defaultValue: Value
    ): this(wrapped, defaultValueSupplier = { defaultValue })

    override fun get(key: Key): Value {
        return wrapped[key] ?: defaultValueSupplier(key)
    }
}

class NonNullDefaultValueMutableMapStore<Key, Value>(
    private val wrapped: MutableMapStore<Key, Value>,
    private val defaultValueSupplier: (Key) -> Value
): NonNullMutableMapStore<Key, Value> {
    override val keys: Set<Key>
        get() = wrapped.keys

    constructor(
        wrapped: MutableMapStore<Key, Value>,
        defaultValue: Value
    ): this(wrapped, defaultValueSupplier = { defaultValue })

    override fun get(key: Key): Value {
        return wrapped[key] ?: defaultValueSupplier(key)
    }

    override fun set(key: Key, value: Value) {
        wrapped[key] = value
    }

    override fun remove(key: Key) {
        wrapped.remove(key)
    }
}

fun <Key, Value: Any> MapStore<Key, Value?>.replacingNull(defaultValueSupplier: (Key) -> Value): NonNullDefaultValueMapStore<Key, Value> {
    return NonNullDefaultValueMapStore(this, defaultValueSupplier)
}

fun <Key, Value: Any> MapStore<Key, Value?>.replacingNull(defaultValue: Value): NonNullDefaultValueMapStore<Key, Value> {
    return NonNullDefaultValueMapStore(this, defaultValue)
}

fun <Key, Value> MutableMapStore<Key, Value>.replacingNull(defaultValueSupplier: (Key) -> Value): NonNullDefaultValueMutableMapStore<Key, Value> {
    return NonNullDefaultValueMutableMapStore(this, defaultValueSupplier)
}

fun <Key, Value> MutableMapStore<Key, Value>.replacingNull(defaultValue: Value): NonNullDefaultValueMutableMapStore<Key, Value> {
    return NonNullDefaultValueMutableMapStore(this, defaultValue)
}
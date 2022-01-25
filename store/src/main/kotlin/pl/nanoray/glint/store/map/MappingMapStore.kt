package pl.nanoray.glint.store.map

class MappingMapStore<Key, T, R>(
    private val wrapped: MapStore<Key, T>,
    private val mapper: (T) -> R
): MapStore<Key, R?> {
    override val keys: Set<Key>
        get() = wrapped.keys

    override fun get(key: Key): R? {
        return mapper(wrapped[key])
    }
}

class MappingMutableMapStore<Key, T, R>(
    private val wrapped: MutableMapStore<Key, T>,
    private val readMapper: (T) -> R,
    private val writeMapper: (R) -> T
): MutableMapStore<Key, R> {
    override val keys: Set<Key>
        get() = wrapped.keys

    override fun get(key: Key): R? {
        return wrapped[key]?.let { readMapper(it) }
    }

    override fun set(key: Key, value: R) {
        wrapped[key] = writeMapper(value)
    }

    override fun remove(key: Key) {
        wrapped.remove(key)
    }
}

class NonNullMappingMutableMapStore<Key, T, R>(
    private val wrapped: NonNullMutableMapStore<Key, T>,
    private val readMapper: (T) -> R,
    private val writeMapper: (R) -> T
): NonNullMutableMapStore<Key, R> {
    override val keys: Set<Key>
        get() = wrapped.keys

    override fun get(key: Key): R {
        return readMapper(wrapped[key])
    }

    override fun set(key: Key, value: R) {
        wrapped[key] = writeMapper(value)
    }

    override fun remove(key: Key) {
        wrapped.remove(key)
    }
}

fun <Key, T, R> MapStore<Key, T>.map(mapper: (T) -> R): MappingMapStore<Key, T, R> {
    return MappingMapStore(this, mapper)
}

fun <Key, T, R> MutableMapStore<Key, T>.map(readMapper: (T) -> R, writeMapper: (R) -> T): MappingMutableMapStore<Key, T, R> {
    return MappingMutableMapStore(this, readMapper, writeMapper)
}

fun <Key, T, R> NonNullMutableMapStore<Key, T>.map(readMapper: (T) -> R, writeMapper: (R) -> T): NonNullMappingMutableMapStore<Key, T, R> {
    return NonNullMappingMutableMapStore(this, readMapper, writeMapper)
}
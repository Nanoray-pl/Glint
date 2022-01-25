package pl.nanoray.glint.store

class MappingStore<T, R>(
    private val wrapped: Store<T>,
    private val mapper: (T) -> R
): Store<R> {
    override val value: R
        get() = mapper(wrapped.value)
}

class MappingMutableStore<T, R>(
    private val wrapped: MutableStore<T>,
    private val readMapper: (T) -> R,
    private val writeMapper: (R) -> T
): MutableStore<R> {
    override var value: R
        get() = readMapper(wrapped.value)
        set(value) { wrapped.value = writeMapper(value) }
}

fun <T, R> Store<T>.map(mapper: (T) -> R): MappingStore<T, R> {
    return MappingStore(this, mapper)
}

fun <T, R> MutableStore<T>.map(readMapper: (T) -> R, writeMapper: (R) -> T): MappingMutableStore<T, R> {
    return MappingMutableStore(this, readMapper, writeMapper)
}
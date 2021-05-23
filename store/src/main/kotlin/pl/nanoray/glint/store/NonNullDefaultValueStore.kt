package pl.nanoray.glint.store

class NonNullDefaultValueStore<T: Any>(
	private val wrapped: Store<T?>,
	private val defaultValueSupplier: () -> T
): Store<T> {
	constructor(
		wrapped: Store<T?>,
		defaultValue: T
	): this(wrapped, defaultValueSupplier = { defaultValue })

	override var value: T
		get() = wrapped.value ?: defaultValueSupplier()
		set(value) { wrapped.value = value }
}

fun <T: Any> Store<T?>.replacingNull(defaultValueSupplier: () -> T): NonNullDefaultValueStore<T> {
	return NonNullDefaultValueStore(this, defaultValueSupplier)
}

fun <T: Any> Store<T?>.replacingNull(defaultValue: T): NonNullDefaultValueStore<T> {
	return NonNullDefaultValueStore(this, defaultValue)
}
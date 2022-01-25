package pl.nanoray.glint.store

class NonNullDefaultValueStore<T: Any>(
	private val wrapped: Store<T?>,
	private val defaultValueSupplier: () -> T
): Store<T> {
	constructor(
		wrapped: Store<T?>,
		defaultValue: T
	): this(wrapped, defaultValueSupplier = { defaultValue })

	override val value: T
		get() = wrapped.value ?: defaultValueSupplier()
}

class NonNullDefaultValueMutableStore<T: Any>(
	private val wrapped: MutableStore<T?>,
	private val defaultValueSupplier: () -> T
): MutableStore<T> {
	constructor(
		wrapped: MutableStore<T?>,
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

fun <T: Any> MutableStore<T?>.replacingNull(defaultValueSupplier: () -> T): NonNullDefaultValueMutableStore<T> {
	return NonNullDefaultValueMutableStore(this, defaultValueSupplier)
}

fun <T: Any> MutableStore<T?>.replacingNull(defaultValue: T): NonNullDefaultValueMutableStore<T> {
	return NonNullDefaultValueMutableStore(this, defaultValue)
}
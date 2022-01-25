package pl.nanoray.glint.store

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class CachingStore<T>(
	private val wrapped: MutableStore<T>
): MutableStore<T> {
	private sealed class Value<T> {
		class Cached<T>(
			val value: T
		): Value<T>()

		class NotCached<T>: Value<T>()
	}

	private val lock = ReentrantLock()
	private var valueStore: Value<T> = Value.NotCached()

	override var value: T
		get() = lock.withLock {
			when (val cachedValue = valueStore) {
				is Value.Cached -> cachedValue.value
				is Value.NotCached -> {
					val wrappedValue = wrapped.value
					valueStore = Value.Cached(wrappedValue)
					return@withLock wrappedValue
				}
			}
		}
		set(value) = lock.withLock {
			valueStore = Value.Cached(value)
			wrapped.value = value
		}
}

fun <T> MutableStore<T>.caching(): CachingStore<T> {
	return CachingStore(this)
}
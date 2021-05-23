package pl.nanoray.glint.store

import java.util.*
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.schedule
import kotlin.concurrent.withLock

class ThrottleStore<T>(
	wrapped: Store<T>,
	private val throttleTimeMs: Long
): Store<T> {
	private val wrapped = wrapped as? CachingStore<T> ?: wrapped.caching()
	private val lock = ReentrantLock()
	private val timer = Timer()

	private var isTimerRunning = false
	private var throttledValue: T? = null

	override var value: T
		get() = lock.withLock { wrapped.value }
		set(value) = lock.withLock {
			throttledValue = value
			if (!isTimerRunning) {
				isTimerRunning = true
				timer.schedule(throttleTimeMs) {
					lock.withLock {
						wrapped.value = throttledValue!!
						throttledValue = null
						isTimerRunning = false
					}
				}
			}
		}

	fun storeNow() {
		lock.withLock {
			if (isTimerRunning) {
				isTimerRunning = false
				timer.cancel()
				wrapped.value = throttledValue!!
				throttledValue = null
			}
		}
	}
}

fun <T> Store<T>.throttling(throttleTimeMs: Long): ThrottleStore<T> {
	return ThrottleStore(this, throttleTimeMs)
}
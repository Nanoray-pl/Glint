package pl.nanoray.glint.utilities

sealed class WithDefault<T> {
	class Default<T>: WithDefault<T>()

	data class NonDefault<T>(
		val value: T
	): WithDefault<T>()

	fun valueOrDefault(default: T): T {
		return when (this) {
			is Default<T> -> default
			is NonDefault<T> -> value
		}
	}
}
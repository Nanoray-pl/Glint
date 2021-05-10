package pl.nanoray.glint.store

import pl.nanoray.glint.plugin.ContainerEnabledPlugin
import pl.shockah.unikorn.dependency.Container

class StorePlugin(
		container: Container
): ContainerEnabledPlugin(container) {
	private val throttleStores = mutableSetOf<ThrottleStore<*>>()

	init {
		register { this }
		Runtime.getRuntime().addShutdownHook(object: Thread() {
			override fun run() {
				super.run()
				throttleStores.forEach { it.storeNow() }
			}
		})
	}

	override fun onUnload() {
		throttleStores.forEach { it.storeNow() }
	}

	fun registerThrottleStore(throttleStore: ThrottleStore<*>) {
		throttleStores.add(throttleStore)
	}

	fun unregisterThrottleStore(throttleStore: ThrottleStore<*>) {
		throttleStores.remove(throttleStore)
	}
}
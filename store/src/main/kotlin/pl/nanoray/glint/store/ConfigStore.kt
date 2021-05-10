package pl.nanoray.glint.store

import pl.nanoray.glint.ConfigManager
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class ConfigStore<T>(
		resolver: Resolver,
		private val type: KType,
		private val configName: String
): Store<T?> {
	companion object {
		inline operator fun <reified T> invoke(resolver: Resolver, configName: String): ConfigStore<T> {
			return ConfigStore(resolver, typeOf<T>(), configName)
		}
	}

	private val configManager: ConfigManager by resolver.inject()

	override var value: T?
		get() = @Suppress("UNCHECKED_CAST") configManager.getConfig(type, configName)?.let { it as T }
		set(value) = configManager.storeConfig(type, configName, value)
}
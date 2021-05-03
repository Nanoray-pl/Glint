package pl.nanoray.glint

import com.sksamuel.hoplite.ConfigLoader
import com.sksamuel.hoplite.ConfigSource
import com.sksamuel.hoplite.fp.Validated
import java.nio.file.Path
import kotlin.reflect.KClass

interface ConfigManager {
	fun <T: Any> getConfig(type: KClass<T>, name: String): T?
}

fun <T: Any> ConfigManager.getConfig(type: KClass<T>): T? {
	return type.qualifiedName?.let { getConfig(type, it) }
}

inline fun <reified T: Any> ConfigManager.getConfig(name: String): T? {
	return getConfig(T::class, name)
}

inline fun <reified T: Any> ConfigManager.getConfig(): T? {
	return getConfig(T::class)
}

class ConfigManagerImpl(
		private val configPath: Path
): ConfigManager {
	private val configLoader = ConfigLoader()

	override fun <T: Any> getConfig(type: KClass<T>, name: String): T? {
		val extensions = listOf("json", "yml", "yaml", "toml", "props", "properties")
		val configSource = when (val configSource = ConfigSource.fromPaths(getPathsForConfig(name, extensions))) {
			is Validated.Invalid -> return null
			is Validated.Valid -> configSource.value
		}
		return configLoader.loadConfig(type, configSource).mapInvalid { null }.getUnsafe()
	}

	private fun getPathsForConfig(name: String, extensions: List<String>): List<Path> {
		return extensions.map { configPath.resolve("${name}.$it") }
	}
}
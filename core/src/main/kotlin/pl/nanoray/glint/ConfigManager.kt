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
		for (extension in extensions) {
			val configSource = when (val configSource = ConfigSource.fromPaths(listOf(configPath.resolve("${name}.${extension}")))) {
				is Validated.Invalid -> continue
				is Validated.Valid -> configSource.value
			}
			return when (val config = configLoader.loadConfig(type, configSource)) {
				is Validated.Valid -> config.value
				is Validated.Invalid -> null
			}
		}
		return null
	}
}
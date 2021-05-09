package pl.nanoray.glint

import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ConfigManager {
	fun <T: Any> getConfig(type: KType, klass: KClass<T>, name: String): T?
}

fun <T: Any> ConfigManager.getConfig(type: KType, klass: KClass<T>): T? {
	return klass.qualifiedName?.let { getConfig(type, klass, it) }
}

inline fun <reified T: Any> ConfigManager.getConfig(name: String): T? {
	return getConfig(typeOf<T>(), T::class, name)
}

inline fun <reified T: Any> ConfigManager.getConfig(): T? {
	return getConfig(typeOf<T>(), T::class)
}

class ConfigManagerImpl(
		private val configPath: Path
): ConfigManager {
	private val jsonFormat = Json {
		prettyPrint = true
		prettyPrintIndent = "\t"
	}

	override fun <T: Any> getConfig(type: KType, klass: KClass<T>, name: String): T? {
//		val extensions = listOf("json", "yml", "yaml", "toml", "props", "properties")
		return getJsonConfig(type, name)
	}

	private fun <T: Any> getJsonConfig(type: KType, name: String): T? {
		val path = configPath.resolve("${name}.json")
		if (!path.exists() || !path.isRegularFile())
			return null
		val stringContent = path.readText()
		val serializer = serializer(type)
		@Suppress("UNCHECKED_CAST")
		return jsonFormat.decodeFromString(serializer, stringContent)?.let { it as T }
	}
}
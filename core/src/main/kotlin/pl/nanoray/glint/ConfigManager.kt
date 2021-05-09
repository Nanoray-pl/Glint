package pl.nanoray.glint

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import java.io.BufferedInputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
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
	private val propertiesFormat = Properties
	private val yamlFormat = Yaml.default

	override fun <T: Any> getConfig(type: KType, klass: KClass<T>, name: String): T? {
		return getJsonConfig(type, name) ?: getYamlConfig(type, name) ?: getPropertiesConfig(type, name)
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

	private fun <T: Any> getYamlConfig(type: KType, name: String): T? {
		for (extension in listOf("yml", "yaml")) {
			val path = configPath.resolve("${name}.$extension")
			if (!path.exists() || !path.isRegularFile())
				return null
			val stringContent = path.readText()
			val serializer = serializer(type)
			@Suppress("UNCHECKED_CAST")
			yamlFormat.decodeFromString(serializer, stringContent)?.let { return it as T }
		}
		return null
	}

	private fun <T: Any> getPropertiesConfig(type: KType, name: String): T? {
		for (extension in listOf("props", "properties")) {
			val path = configPath.resolve("${name}.$extension")
			if (!path.exists() || !path.isRegularFile())
				return null
			val stringContent = path.readText()
			val properties = java.util.Properties().apply { BufferedInputStream(path.inputStream()).use { load(it) } }
			val serializer = serializer(type)
			@Suppress("UNCHECKED_CAST")
			propertiesFormat.decodeFromMap(serializer, properties.toMap() as Map<String, Any>)
			@Suppress("UNCHECKED_CAST")
			yamlFormat.decodeFromString(serializer, stringContent)?.let { return it as T }
		}
		return null
	}
}
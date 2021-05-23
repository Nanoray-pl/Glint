package pl.nanoray.glint

import kotlinx.serialization.json.Json
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer
import net.mamoe.yamlkt.Yaml
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf

interface ConfigManager {
	fun getConfig(type: KType, name: String): Any?
	fun storeConfig(type: KType, name: String, config: Any?)

	fun getConfig(type: KType): Any? {
		return (type.classifier as? KClass<*>)?.qualifiedName?.let { getConfig(type, it) }
	}

	fun storeConfig(type: KType, config: Any?) {
		val name = (type.classifier as? KClass<*>)?.qualifiedName ?: throw IllegalArgumentException()
		storeConfig(type, name, config)
	}
}

inline fun <reified T: Any> ConfigManager.getConfig(name: String): T? {
	return getConfig(typeOf<T>(), name) as? T
}

inline fun <reified T: Any> ConfigManager.getConfig(): T? {
	return getConfig(typeOf<T>()) as? T
}

inline fun <reified T: Any> ConfigManager.storeConfig(name: String, config: T?) {
	storeConfig(typeOf<T>(), name, config)
}

inline fun <reified T: Any> ConfigManager.storeConfig(config: T?) {
	storeConfig(typeOf<T>(), config)
}

internal class ConfigManagerImpl(
	private val configPath: Path
): ConfigManager {
	private val jsonFormat = Json {
		prettyPrint = true
		prettyPrintIndent = "\t"
	}
	private val propertiesFormat = Properties
	private val yamlFormat = Yaml.Default

	override fun getConfig(type: KType, name: String): Any? {
		return getYamlConfig(type, name) ?: getJsonConfig(type, name) ?: getPropertiesConfig(type, name)
	}

	override fun storeConfig(type: KType, name: String, config: Any?) {
		@Suppress("NAME_SHADOWING")
		val configHandlers = listOf<Pair<List<String>, (Path, Any) -> Unit>>(
			listOf("yml", "yaml") to { path, config -> storeYamlConfig(type, path, config) },
			listOf("json") to { path, config -> storeJsonConfig(type, path, config) },
			listOf("props", "properties") to { path, config -> storePropertiesConfig(type, path, config) }
		)
		for ((extensions, configHandler) in configHandlers) {
			for (extension in extensions) {
				val path = configPath.resolve("${name}.$extension")
				if (path.exists()) {
					if (config == null)
						path.deleteIfExists()
					else
						configHandler(path, config)
				}
			}
		}

		val path = configPath.resolve("${name}.yml")
		if (config == null)
			path.deleteIfExists()
		else
			storeYamlConfig(type, path, config)
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

	private fun <T: Any> getJsonConfig(type: KType, name: String): T? {
		val path = configPath.resolve("${name}.json")
		if (!path.exists() || !path.isRegularFile())
			return null
		val stringContent = path.readText()
		val serializer = serializer(type)
		@Suppress("UNCHECKED_CAST")
		return jsonFormat.decodeFromString(serializer, stringContent)?.let { it as T }
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

	private fun <T: Any> storeYamlConfig(type: KType, path: Path, config: T) {
		val serializer = serializer(type)
		val string = yamlFormat.encodeToString(serializer, config)
		path.writeText(string)
	}

	private fun <T: Any> storeJsonConfig(type: KType, path: Path, config: T) {
		val serializer = serializer(type)
		val string = jsonFormat.encodeToString(serializer, config)
		path.writeText(string)
	}

	private fun <T: Any> storePropertiesConfig(type: KType, path: Path, config: T) {
		val serializer = serializer(type)
		val encoded = propertiesFormat.encodeToMap(serializer, config)
		val properties = java.util.Properties().apply { putAll(encoded) }
		BufferedOutputStream(path.outputStream()).use { properties.store(it, null) }
	}
}
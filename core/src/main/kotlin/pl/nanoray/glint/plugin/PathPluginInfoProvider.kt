package pl.nanoray.glint.plugin

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import pl.shockah.unikorn.collection.mapValid
import pl.shockah.unikorn.plugin.PluginInfoProvider
import pl.shockah.unikorn.plugin.impl.FilePluginInfo
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

internal class PathPluginInfoProvider(
	private val pluginPath: Path
): PluginInfoProvider<PathPluginInfo> {
	class MissingPluginDefinitionException: Exception()

	override fun getPluginInfos(): Set<PathPluginInfo> {
		return Files.newDirectoryStream(pluginPath).filter { it.extension == "jar" }.mapValid { readPluginInfo(it) }.toSet()
	}

	private fun readPluginInfo(jarPath: Path): PathPluginInfo {
		require(jarPath.exists() && jarPath.isRegularFile()) { "Plugin JAR file $jarPath doesn't exist." }

		ZipInputStream(BufferedInputStream(Files.newInputStream(jarPath))).use { zip ->
			val handlers: List<Pair<List<String>, (String) -> FilePluginInfo.Base?>> = listOf(
				listOf("yml", "yaml") to { Yaml.default.decodeFromString(it) },
				listOf("json") to { Json.Default.decodeFromString(it) }
			)
			while (true) {
				val zipEntry = zip.nextEntry ?: throw MissingPluginDefinitionException()
				for ((extensions, handler) in handlers) {
					for (extension in extensions) {
						if (zipEntry.name != "plugin.$extension")
							continue
						val content = zip.bufferedReader().use { it.readText() }
						return readPluginInfo(handler(content) ?: continue, jarPath)
					}
				}
			}
		}
	}

	private fun readPluginInfo(basePluginInfo: FilePluginInfo.Base, jarPath: Path): PathPluginInfo {
		return PathPluginInfo(
			basePluginInfo.identifier,
			basePluginInfo.version,
			basePluginInfo.dependencies,
			jarPath,
			basePluginInfo.pluginClassName
		)
	}
}
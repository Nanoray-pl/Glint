package pl.nanoray.glint.plugin

import com.beust.klaxon.JsonObject
import com.beust.klaxon.Klaxon
import pl.shockah.unikorn.collection.mapValid
import pl.shockah.unikorn.plugin.PluginInfo
import pl.shockah.unikorn.plugin.PluginInfoProvider
import pl.shockah.unikorn.plugin.PluginVersion
import java.io.BufferedInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile

class PathPluginInfoProvider(
		private val pluginPath: Path
): PluginInfoProvider<PathPluginInfo> {
	class MissingPluginJsonException: Exception()

	override fun getPluginInfos(): Set<PathPluginInfo> {
		return Files.newDirectoryStream(pluginPath).filter { it.extension == "jar" }.mapValid { readPluginInfo(it) }.toSet()
	}

	private fun readPluginInfo(jarPath: Path): PathPluginInfo {
		require(jarPath.exists() && jarPath.isRegularFile()) { "Plugin JAR file $jarPath doesn't exist." }

		ZipInputStream(BufferedInputStream(Files.newInputStream(jarPath))).use { zip ->
			while (true) {
				(zip.nextEntry ?: throw MissingPluginJsonException()).takeIf { it.name == "plugin.json" } ?: continue
				val json = Klaxon().parseJsonObject(zip.reader())
				return readPluginInfo(json, jarPath)
			}
		}
	}

	private fun readPluginInfo(json: JsonObject, jarPath: Path): PathPluginInfo {
		return PathPluginInfo(
				json.string("identifier")!!,
				PluginVersion(json.string("version") ?: "1.0"),
				json.array<JsonObject>("dependencies")?.map {
					PluginInfo.DependencyEntry(
							it.string("identifier")!!,
							PluginVersion.Filter(it.string("version") ?: "*")
					)
				}?.toSet() ?: emptySet(),
				jarPath,
				json.string("pluginClassName")!!
		)
	}
}
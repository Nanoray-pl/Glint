package pl.nanoray.glint.plugin

import kotlinx.serialization.Serializable
import pl.shockah.unikorn.plugin.PluginInfo
import pl.shockah.unikorn.plugin.PluginVersion
import java.nio.file.Path

data class PathPluginInfo(
		override val identifier: String,
		override val version: PluginVersion = PluginVersion("1.0"),
		override val dependencies: Set<PluginInfo.DependencyEntry> = emptySet(),
		val jarPath: Path,
		val pluginClassName: String
): PluginInfo {
	@Serializable
	data class Base(
			override val identifier: String,
			override val version: PluginVersion = PluginVersion(listOf(1, 0)),
			override val dependencies: Set<PluginInfo.DependencyEntry> = emptySet(),
			val pluginClassName: String
	): PluginInfo
}
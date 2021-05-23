package pl.nanoray.glint.plugin

import pl.shockah.unikorn.plugin.PluginLoader
import pl.shockah.unikorn.plugin.PluginLoaderFactory
import pl.shockah.unikorn.plugin.impl.ClassLoaderPluginLoader
import java.nio.file.FileSystems

internal class PathPluginLoaderFactory: PluginLoaderFactory<PathPluginInfo> {
	override fun createPluginLoader(pluginInfos: Collection<PathPluginInfo>): PluginLoader<PathPluginInfo> {
		val classLoaderPaths = pluginInfos.map {
			val fileSystem = FileSystems.newFileSystem(it.jarPath, null as ClassLoader?)
			return@map fileSystem.rootDirectories.single()
		}
		val classLoader = PathClassLoader(classLoaderPaths, Thread.currentThread().contextClassLoader)
		return ClassLoaderPluginLoader(classLoader) { it.pluginClassName }
	}
}
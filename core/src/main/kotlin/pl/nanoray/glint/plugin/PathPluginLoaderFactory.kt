package pl.nanoray.glint.plugin

import com.github.marschall.pathclassloader.PathClassLoader
import pl.shockah.unikorn.plugin.PluginLoader
import pl.shockah.unikorn.plugin.PluginLoaderFactory
import pl.shockah.unikorn.plugin.impl.ClassLoaderPluginLoader
import java.nio.file.FileSystems

class PathPluginLoaderFactory: PluginLoaderFactory<PathPluginInfo> {
	override fun createPluginLoader(pluginInfos: Set<PathPluginInfo>): PluginLoader<PathPluginInfo> {
		var currentClassLoader = Thread.currentThread().contextClassLoader
		for (pluginInfo in pluginInfos) {
			val fileSystem = FileSystems.newFileSystem(pluginInfo.jarPath, null)
			val rootPath = fileSystem.rootDirectories.single()
			currentClassLoader = PathClassLoader(rootPath, currentClassLoader)
		}
		return ClassLoaderPluginLoader(currentClassLoader) { it.pluginClassName }
	}
}
package pl.nanoray.glint.plugin

import java.net.URL
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isRegularFile
import kotlin.io.path.readBytes

class PathClassLoader(
	val paths: List<Path>,
	parent: ClassLoader? = null
): ClassLoader(parent) {
	override fun findClass(name: String?): Class<*> {
		try {
			return super.findClass(name)
		} catch (_: ClassNotFoundException) {
			if (name == null)
				return super.findClass(name)
			for (path in paths) {
				val filePath = path.resolve("${name.replace(".", "/")}.class")
				if (filePath.exists() && filePath.isRegularFile()) {
					val bytes = filePath.readBytes()
					return defineClass(name, bytes, 0, bytes.size)
				}
			}
			throw ClassNotFoundException()
		}
	}

	override fun findResource(name: String?): URL {
		super.findResource(name)?.let { return it }
		if (name == null)
			return super.findResource(name)
		for (path in paths) {
			val filePath = path.resolve(name)
			if (filePath.exists())
				return filePath.toUri().toURL()
		}
		throw ClassNotFoundException()
	}
}
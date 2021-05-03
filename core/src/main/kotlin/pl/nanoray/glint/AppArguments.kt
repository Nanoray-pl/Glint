package pl.nanoray.glint

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import java.nio.file.Path
import java.nio.file.Paths

interface AppArguments {
	val configPath: Path
	val pluginPath: Path
}

class ParsedAppArguments(parser: ArgParser): AppArguments {
	override val configPath: Path by parser.storing(
			"Config path",
			transform = { Paths.get(this) }
	).default(Paths.get("config"))

	override val pluginPath: Path by parser.storing(
			"Plugin path",
			transform = { Paths.get(this) }
	).default(Paths.get("plugins"))
}
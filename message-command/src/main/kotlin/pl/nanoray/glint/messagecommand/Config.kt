package pl.nanoray.glint.messagecommand

data class Config(
		val commandPrefixes: List<String> = emptyList(),
		val commandRegexes: List<Regex> = emptyList()
)
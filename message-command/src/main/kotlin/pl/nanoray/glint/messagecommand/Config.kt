@file:UseSerializers(RegexSerializer::class)

package pl.nanoray.glint.messagecommand

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.utilities.RegexSerializer

@Serializable
data class Config(
		val commandPrefixes: List<String> = emptyList(),
		val commandRegexes: List<Regex> = emptyList()
)
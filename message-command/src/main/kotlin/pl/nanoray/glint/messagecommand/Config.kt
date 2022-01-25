@file:UseSerializers(RegexSerializer::class)

package pl.nanoray.glint.messagecommand

import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import pl.nanoray.glint.jdaextensions.CategoryIdentifier
import pl.nanoray.glint.jdaextensions.GuildIdentifier
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.utilities.RegexSerializer

@Serializable
internal data class Config(
	val global: Entry = Entry(commandPrefixes = listOf("\\")),
	val perGuild: Map<GuildIdentifier, Entry> = emptyMap(),
	val perCategory: Map<CategoryIdentifier, Entry> = emptyMap(),
	val perTextChannel: Map<TextChannelIdentifier, Entry> = emptyMap()
) {
	@Serializable
	internal data class Entry(
		val commandPrefixes: List<String> = emptyList(),
		val commandRegexes: List<Regex> = emptyList()
	)
}
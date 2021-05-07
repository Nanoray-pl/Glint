package pl.nanoray.glint.slashcommand

import pl.nanoray.glint.jdaextensions.GuildIdentifier

interface SlashCommandProvider {
	val globalSlashCommands: Set<SlashCommand>
		get() = emptySet()

	fun getGuildSlashCommands(guild: GuildIdentifier): Set<SlashCommand> {
		return emptySet()
	}
}
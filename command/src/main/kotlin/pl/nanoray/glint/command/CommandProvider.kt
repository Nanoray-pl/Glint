package pl.nanoray.glint.command

import pl.nanoray.glint.jdaextensions.GuildIdentifier

interface CommandProvider {
	val globalCommands: Set<Command>
		get() = emptySet()

	fun getGuildCommands(guild: GuildIdentifier): Set<Command> {
		return emptySet()
	}
}
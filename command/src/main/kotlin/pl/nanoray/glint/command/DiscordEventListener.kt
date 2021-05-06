package pl.nanoray.glint.command

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pl.nanoray.glint.jdaextensions.identifier
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal class DiscordEventListener(
		resolver: Resolver
): ListenerAdapter() {
	private val jda: JDA by resolver.inject()
	private val commandManager: CommandManager by resolver.inject()
	private val commandDataParser: CommandDataParser by resolver.inject()

	override fun onReady(event: ReadyEvent) {
		commandManager.updateGlobalCommands()
				.subscribe()
	}

	override fun onGuildReady(event: GuildReadyEvent) {
		commandManager.updateGuildCommands(event.guild.identifier)
				.subscribe()
	}

	override fun onSlashCommand(event: SlashCommandEvent) {
		@Suppress("UNCHECKED_CAST")
		val command = commandManager.getMatchingCommand(event) as? Command.Simple<Any> ?: throw IllegalArgumentException("Unknown command ${event.commandPath}.")
		val options = commandDataParser.parseCommandOptions(event, command.optionsKlass)
		command.handleCommand(event, options)
	}
}
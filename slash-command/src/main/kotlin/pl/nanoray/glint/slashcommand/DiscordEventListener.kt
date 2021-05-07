package pl.nanoray.glint.slashcommand

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
	private val slashCommandManager: SlashCommandManager by resolver.inject()
	private val slashCommandDataParser: SlashCommandDataParser by resolver.inject()

	override fun onReady(event: ReadyEvent) {
		slashCommandManager.updateGlobalSlashCommands()
				.subscribe()
	}

	override fun onGuildReady(event: GuildReadyEvent) {
		slashCommandManager.updateGuildSlashCommands(event.guild.identifier)
				.subscribe()
	}

	override fun onSlashCommand(event: SlashCommandEvent) {
		@Suppress("UNCHECKED_CAST")
		val command = slashCommandManager.getMatchingSlashCommand(event) as? SlashCommand.Simple<Any> ?: throw IllegalArgumentException("Unknown command ${event.commandPath}.")
		val options = slashCommandDataParser.parseSlashCommandOptions(event, command.optionsKlass)
		command.handleCommand(event, options)
	}
}
package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.guild.GuildReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent
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
	private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()
	private val discordWorker: DiscordWorker by resolver.inject()

	init {
		if (jda.status == JDA.Status.CONNECTED)
			commandManager.setupCommandsInAllGuilds()
					.subscribe()
	}

	override fun onGenericEvent(event: GenericEvent) {
		if (event is GenericGuildVoiceUpdateEvent) {
			discordWorker.updateAccess(event.channelLeft?.identifier, event.channelJoined?.identifier, event.guild.identifier, event.member.user.identifier)
		}
	}

	override fun onGuildReady(event: GuildReadyEvent) {
		commandManager.setupCommandsInGuild(event.guild.identifier)
				.subscribe()
	}

	override fun onSlashCommand(event: SlashCommandEvent) {
		commandManager.handleCommandEvent(event)
	}
}
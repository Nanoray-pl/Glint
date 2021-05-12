package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.channel.text.TextChannelDeleteEvent
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent
import net.dv8tion.jda.api.hooks.ListenerAdapter
import pl.nanoray.glint.jdaextensions.identifier
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal class DiscordEventListener(
		resolver: Resolver
): ListenerAdapter() {
	private val jda: JDA by resolver.inject()
	private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()
	private val discordWorker: DiscordWorker by resolver.inject()

	override fun onReady(event: ReadyEvent) {
		discordWorker.cleanUpStaleVoiceTextChannelMappings().subscribe()
	}

	override fun onGenericEvent(event: GenericEvent) {
		if (event is GenericGuildVoiceUpdateEvent) {
			discordWorker
					.updateAccess(event.channelLeft, event.channelJoined, event.guild, event.member)
					.subscribe()
		}
	}

	override fun onTextChannelDelete(event: TextChannelDeleteEvent) {
		voiceTextChannelManager.voiceTextChannelMappings.firstOrNull { it.textChannel == event.channel.identifier }?.let {
			voiceTextChannelManager.unlinkTextChannelFromVoiceChannel(it.textChannel).subscribe()
		}
	}

	override fun onVoiceChannelDelete(event: VoiceChannelDeleteEvent) {
		voiceTextChannelManager.voiceTextChannelMappings.firstOrNull { it.voiceChannel == event.channel.identifier }?.let {
			voiceTextChannelManager.unlinkVoiceChannelFromTextChannel(it.voiceChannel).subscribe()
		}
	}
}
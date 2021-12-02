package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.channel.ChannelDeleteEvent
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
			val channelLeft = event.channelLeft as? VoiceChannel?
			val channelJoined = event.channelLeft as? VoiceChannel?
			if (channelLeft == null && channelJoined == null)
				throw IllegalArgumentException("The fuck is this state") // TODO: log instead of throwing
			discordWorker
				.updateAccess(channelLeft, channelJoined, event.guild, event.member)
				.subscribe()
		}
	}

	override fun onChannelDelete(event: ChannelDeleteEvent) {
		when (val channel = event.channel) {
			is TextChannel -> {
				voiceTextChannelManager.voiceTextChannelMappings.firstOrNull { it.textChannel == channel.identifier }?.let {
					voiceTextChannelManager.unlinkTextChannelFromVoiceChannel(it.textChannel).subscribe()
				}
			}
			is VoiceChannel -> {
				voiceTextChannelManager.voiceTextChannelMappings.firstOrNull { it.voiceChannel == channel.identifier }?.let {
					voiceTextChannelManager.unlinkVoiceChannelFromTextChannel(it.voiceChannel).subscribe()
				}
			}
		}
	}
}
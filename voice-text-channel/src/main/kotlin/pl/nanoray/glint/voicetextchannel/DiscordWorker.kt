package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.VoiceChannel
import pl.nanoray.glint.jdaextensions.getTextChannel
import pl.nanoray.glint.jdaextensions.getVoiceChannel
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.jdaextensions.toSingle
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import javax.annotation.CheckReturnValue

internal interface DiscordWorker {
	@CheckReturnValue fun cleanUpStaleVoiceTextChannelMappings(): Completable
	@CheckReturnValue fun grantAccess(mapping: ChannelMapping, guild: Guild, member: Member): Completable
	@CheckReturnValue fun denyAccess(mapping: ChannelMapping, guild: Guild, member: Member): Completable
	@CheckReturnValue fun updateAccess(channelLeft: VoiceChannel?, channelJoined: VoiceChannel?, guild: Guild, member: Member): Completable
}

internal class DiscordWorkerImpl(
	resolver: Resolver
): DiscordWorker {
	private val jda: JDA by resolver.inject()
	private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()

	@CheckReturnValue
	override fun cleanUpStaleVoiceTextChannelMappings(): Completable {
		val completables = mutableListOf<Completable>()
		for (mapping in voiceTextChannelManager.voiceTextChannelMappings) {
			if (jda.getVoiceChannel(mapping.voiceChannel) == null) {
				completables += voiceTextChannelManager.unlinkVoiceChannelFromTextChannel(mapping.voiceChannel)
				continue
			}
			if (jda.getTextChannel(mapping.textChannel) == null) {
				completables += voiceTextChannelManager.unlinkTextChannelFromVoiceChannel(mapping.textChannel)
				continue
			}
		}
		return Completable.merge(completables)
	}

	@CheckReturnValue
	override fun grantAccess(mapping: ChannelMapping, guild: Guild, member: Member): Completable {
		val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalArgumentException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
		return textChannel.upsertPermissionOverride(member)
			.grant(Permission.VIEW_CHANNEL)
			.toSingle()
			.ignoreElement()
	}

	@CheckReturnValue
	override fun denyAccess(mapping: ChannelMapping, guild: Guild, member: Member): Completable {
		val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalArgumentException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
		return textChannel.upsertPermissionOverride(member)
			.clear(Permission.VIEW_CHANNEL)
			.toSingle()
			.ignoreElement()
	}

	@CheckReturnValue
	override fun updateAccess(channelLeft: VoiceChannel?, channelJoined: VoiceChannel?, guild: Guild, member: Member): Completable {
		require(channelLeft != null || channelJoined != null)
		val completables = mutableListOf<Completable>()
		channelLeft?.let { voiceTextChannelManager.getMappingForVoiceChannel(it.identifier) }?.let { completables.add(denyAccess(it, guild, member)) }
		channelJoined?.let { voiceTextChannelManager.getMappingForVoiceChannel(it.identifier) }?.let { completables.add(grantAccess(it, guild, member)) }
		return Completable.merge(completables)
	}
}
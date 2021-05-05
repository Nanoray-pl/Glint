package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import pl.nanoray.glint.jdaextensions.*
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal interface DiscordWorker {
	fun grantAccess(mapping: ChannelMapping, guild: GuildIdentifier, member: UserIdentifier): Completable
	fun denyAccess(mapping: ChannelMapping, guild: GuildIdentifier, member: UserIdentifier): Completable
	fun updateAccess(channelLeft: VoiceChannelIdentifier?, channelJoined: VoiceChannelIdentifier?, guild: GuildIdentifier, member: UserIdentifier): Completable
}

internal class DiscordWorkerImpl(
		resolver: Resolver
): DiscordWorker {
	private val jda: JDA by resolver.inject()
	private val voiceTextChannelManager: VoiceTextChannelManager by resolver.inject()

	override fun grantAccess(mapping: ChannelMapping, guild: GuildIdentifier, member: UserIdentifier): Completable {
		val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalArgumentException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
		val guildEntity = requireNotNull(jda.getGuild(guild))
		val memberEntity = requireNotNull(guildEntity.getMember(member))
		return textChannel.upsertPermissionOverride(memberEntity)
				.grant(Permission.VIEW_CHANNEL)
				.asSingle()
				.ignoreElement()
	}

	override fun denyAccess(mapping: ChannelMapping, guild: GuildIdentifier, member: UserIdentifier): Completable {
		val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalArgumentException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
		val guildEntity = requireNotNull(jda.getGuild(guild))
		val memberEntity = requireNotNull(guildEntity.getMember(member))
		return textChannel.upsertPermissionOverride(memberEntity)
				.clear(Permission.VIEW_CHANNEL)
				.asSingle()
				.ignoreElement()
	}

	override fun updateAccess(channelLeft: VoiceChannelIdentifier?, channelJoined: VoiceChannelIdentifier?, guild: GuildIdentifier, member: UserIdentifier): Completable {
		require(channelLeft != null || channelJoined != null)
		val completables = mutableListOf<Completable>()
		channelLeft?.let { voiceTextChannelManager.getMappingForVoiceChannel(it) }?.let { completables.add(denyAccess(it, guild, member)) }
		channelJoined?.let { voiceTextChannelManager.getMappingForVoiceChannel(it) }?.let { completables.add(grantAccess(it, guild, member)) }
		return when (completables.size) {
			0 -> Completable.complete()
			1 -> completables.first()
			else -> Completable.merge(completables)
		}
	}
}
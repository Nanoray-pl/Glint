package pl.nanoray.glint.spammoderation

import net.dv8tion.jda.api.JDA
import pl.nanoray.glint.jdaextensions.*

internal interface DiscordModerationWorker {
	fun removeMessages(messages: Collection<Pair<TextChannelIdentifier, MessageIdentifier>>)
	fun kickUser(userId: UserIdentifier, guildId: GuildIdentifier, reason: String)
	fun banUser(userId: UserIdentifier, guildId: GuildIdentifier, daysOfMessagesToDelete: Int, reason: String)
}

internal class DiscordModerationWorkerImpl(
	private val jda: JDA
): DiscordModerationWorker {
	override fun removeMessages(messages: Collection<Pair<TextChannelIdentifier, MessageIdentifier>>) {
		val channelMessages = messages.groupBy { it.first }.mapValues { it.value.map { it.second } }
		channelMessages.forEach { (channelId, messageIds) ->
			val channel = jda.getTextChannel(channelId) ?: throw IllegalArgumentException()
			channel.deleteMessagesByIds(messageIds.map { "${it.value}" }).queue()
		}
	}

	override fun kickUser(userId: UserIdentifier, guildId: GuildIdentifier, reason: String) {
		val guild = jda.getGuild(guildId) ?: throw IllegalArgumentException()
		guild.kick(userId, reason).queue()
	}

	override fun banUser(userId: UserIdentifier, guildId: GuildIdentifier, daysOfMessagesToDelete: Int, reason: String) {
		val guild = jda.getGuild(guildId) ?: throw IllegalArgumentException()
		guild.ban(userId, daysOfMessagesToDelete, reason).queue()
	}
}
package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.EmbedBuilder
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.command.CommandPredicate
import pl.nanoray.glint.jdaextensions.*
import pl.nanoray.glint.messagecommand.MessageCommand
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject
import kotlin.reflect.typeOf

internal class VoiceTextCommand(
		resolver: Resolver
): MessageCommand<Unit>(typeOf<Unit>()) {
	override val name = "voicetext"
	override val description = "List all of the linked voice + text channel pairs."
	override val subcommands = listOf(Link(), Unlink())
	override val predicates = listOf(CommandPredicate.HasGuildPermissions(Permission.ADMINISTRATOR))

	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()
	private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()

	override fun handleCommand(message: Message, options: Unit) {
		message.reply(
				EmbedBuilder()
						.setTitle("Voice + text mappings")
						.appendDescription(voiceTextChannelManager.voiceTextChannelMappings.mapNotNull {
							val voiceChannel = jda.getVoiceChannel(it.voiceChannel) ?: return@mapNotNull null
							val textChannel = jda.getTextChannel(it.textChannel) ?: return@mapNotNull null
							return@mapNotNull "${voiceChannel.asMention} ↔ ${textChannel.asMention}"
						}.joinToString("\n"))
						.build()
		).queue()
	}

	data class LinkOptions(
			@Option.Named("voiceChannel", "v", "Voice channel to link to.") val voiceChannel: VoiceChannelIdentifier? = null,
			@Option.Named("textChannel", "t", "Text channel to link.") val textChannel: TextChannelIdentifier,
			@Option.Named("historyDuration", "d", "Duration to keep the messages for.") val historyDuration: String? = null
	)

	inner class Link: MessageCommand<LinkOptions>(typeOf<LinkOptions>()) {
		override val name = "link"
		override val description = "Link a text channel to a voice channel."
		override val additionalDescription = "`voiceChannel` defaults to the voice channel you are currently connected to."

		override fun handleCommand(message: Message, options: LinkOptions) {
			val voiceChannel = options.voiceChannel ?: (message.channel as? GuildChannel)?.guild?.getMember(message.author)?.voiceState?.channel?.identifier ?: throw IllegalArgumentException("Unknown voice channel.")
			val historyDuration = options.historyDuration?.let { durationParser.parseDuration(it) }
			voiceTextChannelManager.linkTextChannelToVoiceChannel(
					options.textChannel,
					voiceChannel,
					WithDefault.NonDefault(historyDuration ?: VoiceTextChannelDefaults.historyDuration)
			).subscribe(
					{
						message.addReaction("\uD83D\uDC4D").queue()
					},
					{
						message.reply("There was an error: ${it.message}")
					}
			)
		}
	}

	data class UnlinkOptions(
			@Option.Final("channel", "Text/voice channel to unlink.") val channel: GuildChannel? = null
	)

	inner class Unlink: MessageCommand<UnlinkOptions>(typeOf<UnlinkOptions>()) {
		override val name = "unlink"
		override val description = "Unlink a text channel from a voice channel."

		override fun handleCommand(message: Message, options: UnlinkOptions) {
			when (val channel = options.channel ?: (message.channel as? GuildChannel)?.guild?.getMember(message.author)?.voiceState?.channel) {
				is VoiceChannel -> voiceTextChannelManager.unlinkVoiceChannelFromTextChannel(channel.identifier)
						.andThen { message.addReaction("\uD83D\uDC4D").asSingle().ignoreElement() }
						.onErrorResumeNext { message.reply("There was an error: ${it.message}.").asSingle().ignoreElement() }
						.subscribe()
				is TextChannel -> voiceTextChannelManager.unlinkTextChannelFromVoiceChannel(channel.identifier)
						.andThen { message.addReaction("\uD83D\uDC4D").asSingle().ignoreElement() }
						.onErrorResumeNext { message.reply("There was an error: ${it.message}.").asSingle().ignoreElement() }
						.subscribe()
				else -> message.reply("`channel` must be a text or voice channel.").queue()
			}
		}
	}
}
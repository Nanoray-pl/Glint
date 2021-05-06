package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.entities.GuildChannel
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.command.Command
import pl.nanoray.glint.command.CommandOption
import pl.nanoray.glint.jdaextensions.TextChannelIdentifier
import pl.nanoray.glint.jdaextensions.VoiceChannelIdentifier
import pl.nanoray.glint.jdaextensions.asSingle
import pl.nanoray.glint.jdaextensions.identifier
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal class VoiceTextCommand(
		resolver: Resolver
): Command.WithSubcommands() {
	override val name = "voicetext"
	override val description = "[Administrator] Configure a text channel linked to a voice channel."
	override val subcommands = listOf(LinkCommand(resolver), UnlinkCommand(resolver))

	class LinkCommand(
			resolver: Resolver
	): Command.Simple<LinkCommand.Options>(Options::class) {
		override val name = "link"
		override val description = "[Administrator] Link a text channel to a voice channel."

		data class Options(
				@CommandOption("text-channel", "Text channel to link.") val textChannel: TextChannelIdentifier,
				@CommandOption("voice-channel", "Voice channel to link to.") val voiceChannel: VoiceChannelIdentifier? = null,
				@CommandOption("history-duration", "Duration to keep the messages for.") val historyDuration: String? = null
		)

		private val durationParser: DurationParser by resolver.inject()
		private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()

		override fun handleCommand(event: SlashCommandEvent, options: Options) {
			// TODO: Handle optional voiceChannel by checking user's current voice channel.
			val voiceChannel = options.voiceChannel ?: event.member?.voiceState?.channel?.identifier ?: throw IllegalArgumentException("Unknown voice channel.")
			val historyDuration = options.historyDuration?.let { durationParser.parseDuration(it) }
			voiceTextChannelManager.linkTextChannelToVoiceChannel(
					options.textChannel,
					voiceChannel,
					WithDefault.NonDefault(ChannelMapping.Configuration(
							historyDuration ?: VoiceTextChannelDefaults.mappingConfiguration.historyDuration
					))
			)
					.doOnComplete {
						event.reply("Done.")
								.setEphemeral(true)
								.queue()
					}
					.doOnError {
						event.reply("There was an error: ${it.message}.")
								.setEphemeral(true)
								.queue()
					}
					.subscribe()
		}
	}

	class UnlinkCommand(
			resolver: Resolver
	): Command.Simple<UnlinkCommand.Options>(Options::class) {
		override val name = "unlink"
		override val description = "[Administrator] Unlink a text channel from a voice channel."

		data class Options(
				@CommandOption("channel", "Text/voice channel to unlink.") val channel: GuildChannel? = null
		)

		private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()

		override fun handleCommand(event: SlashCommandEvent, options: Options) {
			when (val channel = options.channel) {
				is VoiceChannel -> voiceTextChannelManager.unlinkVoiceChannelFromTextChannel(channel.identifier)
						.andThen { event.reply("Done").setEphemeral(true).asSingle().ignoreElement() }
						.onErrorResumeNext { event.reply("There was an error: ${it.message}.").setEphemeral(true).asSingle().ignoreElement() }
						.subscribe()
				is TextChannel -> voiceTextChannelManager.unlinkTextChannelFromVoiceChannel(channel.identifier)
						.andThen { event.reply("Done").setEphemeral(true).asSingle().ignoreElement() }
						.onErrorResumeNext { event.reply("There was an error: ${it.message}.").setEphemeral(true).asSingle().ignoreElement() }
						.subscribe()
				else -> event.reply("`channel` must be a text or voice channel.")
						.setEphemeral(true)
						.queue()
			}
		}
	}
}
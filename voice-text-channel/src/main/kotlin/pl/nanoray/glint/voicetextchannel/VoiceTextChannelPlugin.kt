package pl.nanoray.glint.voicetextchannel

import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.VoiceChannel
import net.dv8tion.jda.api.events.GenericEvent
import net.dv8tion.jda.api.events.ReadyEvent
import net.dv8tion.jda.api.events.guild.voice.GenericGuildVoiceUpdateEvent
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.hooks.EventListener
import net.dv8tion.jda.api.interactions.commands.OptionType
import net.dv8tion.jda.api.interactions.commands.build.CommandData
import net.dv8tion.jda.api.interactions.commands.build.OptionData
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData
import net.dv8tion.jda.api.requests.RestAction
import pl.nanoray.glint.ContainerEnabledPlugin
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.jdaextensions.*
import pl.nanoray.glint.utilities.WithDefault
import pl.shockah.unikorn.collection.removeFirst
import pl.shockah.unikorn.dependency.Container
import pl.shockah.unikorn.dependency.getValue
import pl.shockah.unikorn.plugin.PluginInfo
import pl.shockah.unikorn.plugin.PluginManager
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.CheckReturnValue
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.time.toDuration

class VoiceTextChannelPlugin(
		manager: PluginManager,
		info: PluginInfo,
		container: Container
): ContainerEnabledPlugin(manager, info, container), MutableVoiceTextChannelManager {
	private val eventListener = PrivateEventListener()
	private val lock = ReentrantReadWriteLock()
	private val observers = mutableListOf<VoiceTextChannelManagerObserver>()
	private val mappings = mutableSetOf<ChannelMapping>()

	private var defaultMappingConfiguration = ChannelMapping.Configuration(
			historyDuration = 15.toDuration(TimeUnit.MINUTES)
	)

	private val jda: JDA by resolver
	private val durationParser: DurationParser by resolver

	init {
		jda.addEventListener(eventListener)
		if (jda.status == JDA.Status.CONNECTED)
			setupCommandsInAllGuilds()
	}

	override fun onUnload() {
		jda.removeEventListener(eventListener)
	}

	override fun addVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.add(observer) }
	}

	override fun removeVoiceTextChannelObserver(observer: VoiceTextChannelManagerObserver) {
		lock.write { observers.remove(observer) }
	}

	override fun getVoiceTextChannelMappings(): Set<ChannelMapping> {
		return lock.read { mappings.toSet() }
	}

	override fun getMappingForVoiceChannel(voiceChannel: VoiceChannelIdentifier): ChannelMapping? {
		return lock.read { mappings.firstOrNull { it.voiceChannel == voiceChannel } }
	}

	override fun getMappingForTextChannel(textChannel: TextChannelIdentifier): ChannelMapping? {
		return lock.read { mappings.firstOrNull { it.voiceChannel == textChannel } }
	}

	override fun linkTextChannelToVoiceChannel(
			textChannel: TextChannelIdentifier,
			voiceChannel: VoiceChannelIdentifier,
			configuration: WithDefault<ChannelMapping.Configuration>
	) {
		lock.write {
			val textChannelMapping = mappings.removeFirst { it.textChannel == textChannel }
			val voiceChannelMapping = mappings.removeFirst { it.voiceChannel == voiceChannel }

			if (textChannelMapping != null)
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, textChannelMapping) }
			if (voiceChannelMapping != null)
				observers.forEach { it.onVoiceTextChannelMappingRemoved(this, voiceChannelMapping) }

			val newMapping = ChannelMapping(
					voiceChannel,
					textChannel,
					configuration.valueOrDefault(defaultMappingConfiguration)
			)
			mappings.add(newMapping)
			observers.forEach { it.onVoiceTextChannelMappingAdded(this, newMapping) }
		}
	}

	override fun createLinkedTextChannelForVoiceChannel(voiceChannel: VoiceChannelIdentifier, configuration: WithDefault<ChannelMapping.Configuration>): TextChannelIdentifier {
		TODO("not implemented")
	}

	override fun unlinkTextChannelFromVoiceChannel(textChannel: TextChannelIdentifier) {
		lock.write {
			val mapping = mappings.removeFirst { it.textChannel == textChannel } ?: return
			observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
			// TODO: Reset permission overrides
		}
	}

	override fun unlinkVoiceChannelFromTextChannel(voiceChannel: VoiceChannelIdentifier) {
		lock.write {
			val mapping = mappings.removeFirst { it.voiceChannel == voiceChannel } ?: return
			observers.forEach { it.onVoiceTextChannelMappingRemoved(this, mapping) }
			// TODO: Reset permission overrides
		}
	}

	@CheckReturnValue
	private fun setupCommandsInAllGuilds(): RestAction<Unit> {
		return jda.guilds
				.map { setupCommandsInGuild(it) }
				.reduce { lhs, rhs -> lhs.and(rhs).map { } }
	}

	@CheckReturnValue
	private fun setupCommandsInGuild(guild: Guild): RestAction<Unit> {
		return guild.retrieveCommands()
				.flatMap {
					val deleteCommands = it.map { guild.deleteCommand(it.identifier) }
					return@flatMap deleteCommands.reduce { lhs, rhs -> lhs.and(rhs).map { } }
				}
				.flatMap {
					guild.upsertCommand(
							CommandData("voicetext", "Configure a text channel linked to a voice channel.")
									.addSubcommand(
											SubcommandData("create", "Create a text channel linked to a voice channel.")
													.addOption(OptionData(OptionType.CHANNEL, "voiceChannel", "Voice channel to link to.").setRequired(false))
									)
									.addSubcommand(
											SubcommandData("link", "Link a text channel to a voice channel.")
													.addOption(OptionData(OptionType.CHANNEL, "textChannel", "Text channel to link.").setRequired(true))
													.addOption(OptionData(OptionType.CHANNEL, "voiceChannel", "Voice channel to link to.").setRequired(false))
													.addOption(OptionData(OptionType.STRING, "historyDuration", "Duration to keep the messages for.").setRequired(false))
									)
									.addSubcommand(
											SubcommandData("unlink", "Unlink a text channel from a voice channel.")
													.addOption(OptionData(OptionType.CHANNEL, "channel", "Text/voice channel to unlink.").setRequired(true))
									)
					)
				}
				.map { }
	}

	private inner class PrivateEventListener: EventListener {
		override fun onEvent(event: GenericEvent) {
			when (event) {
				is ReadyEvent -> {
					setupCommandsInAllGuilds().queue()
				}
				is SlashCommandEvent -> {
					when (event.commandPath) {
						"voicetext/create" -> handleVoicetextCreateCommand(event)
						"voicetext/link" -> handleVoicetextLinkCommand(event)
						"voicetext/unlink" -> handleVoicetextUnlinkCommand(event)
						else -> throw IllegalArgumentException("Unrecognized slash command ${event.commandPath}")
					}
				}
				is GenericGuildVoiceUpdateEvent -> {
					updateAccess(event.channelLeft?.identifier, event.channelJoined?.identifier, event.member)
				}
			}
		}

		private fun handleVoicetextCreateCommand(event: SlashCommandEvent) {
			// TODO: Handle optional voiceChannel by checking user's current voice channel.
			val voiceChannel = event.getOption("voiceChannel")?.asGuildChannel as? VoiceChannel ?: throw IllegalArgumentException("Channel is not a voice channel.")
			TODO("Create a new text channel for $voiceChannel")
			// TODO: Acknowledge/reply
		}

		private fun handleVoicetextLinkCommand(event: SlashCommandEvent) {
			// TODO: Handle optional voiceChannel by checking user's current voice channel.
			val textChannel = event.getOption("textChannel")?.asGuildChannel as? TextChannel ?: throw IllegalArgumentException("Channel is not a text channel.")
			val voiceChannel = event.getOption("voiceChannel")?.asGuildChannel as? VoiceChannel ?: throw IllegalArgumentException("Channel is not a voice channel.")
			val historyDuration = event.getOption("historyDuration")?.asString?.let { durationParser.parseDuration(it) ?: throw IllegalArgumentException("Invalid duration.") }
			linkTextChannelToVoiceChannel(
					textChannel.identifier,
					voiceChannel.identifier,
					WithDefault.NonDefault(ChannelMapping.Configuration(
							historyDuration ?: defaultMappingConfiguration.historyDuration
					))
			)
			event.acknowledge()
		}

		private fun handleVoicetextUnlinkCommand(event: SlashCommandEvent) {
			when (val channel = event.getOption("textChannel")?.asGuildChannel) {
				is VoiceChannel -> unlinkVoiceChannelFromTextChannel(channel.identifier)
				is TextChannel -> unlinkTextChannelFromVoiceChannel(channel.identifier)
				else -> throw IllegalArgumentException("Channel is not a voice or text channel.")
			}
			// TODO: Acknowledge/reply
		}

		private fun grantAccess(mapping: ChannelMapping, member: Member) {
			val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalStateException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
			textChannel.upsertPermissionOverride(member)
					.grant(Permission.VIEW_CHANNEL)
					.queue()
		}

		private fun denyAccess(mapping: ChannelMapping, member: Member) {
			val textChannel = jda.getTextChannel(mapping.textChannel) ?: throw IllegalStateException("Missing text channel ${mapping.textChannel} mapped to voice channel ${mapping.voiceChannel}.")
			textChannel.upsertPermissionOverride(member)
					.clear(Permission.VIEW_CHANNEL)
					.queue()
		}

		private fun updateAccess(channelLeft: VoiceChannelIdentifier?, channelJoined: VoiceChannelIdentifier?, member: Member) {
			require(channelLeft != null || channelJoined != null)
			lock.read {
				mappings.firstOrNull { it.voiceChannel == channelLeft }?.let { denyAccess(it, member) }
				mappings.firstOrNull { it.voiceChannel == channelJoined }?.let { grantAccess(it, member) }
			}
		}
	}
}
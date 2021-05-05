package pl.nanoray.glint.voicetextchannel

import io.reactivex.rxjava3.core.Completable
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import pl.nanoray.glint.DurationParser
import pl.nanoray.glint.jdaextensions.*
import pl.nanoray.glint.logic.Command
import pl.nanoray.glint.logic.CommandDataParser
import pl.nanoray.glint.logic.getCommandData
import pl.nanoray.glint.logic.getCommandMatchingPath
import pl.nanoray.glint.voicetextchannel.command.VoiceTextCommand
import pl.shockah.unikorn.dependency.Resolver
import pl.shockah.unikorn.dependency.inject

internal interface CommandManager {
	fun setupCommandsInAllGuilds(): Completable
	fun setupCommandsInGuild(guild: GuildIdentifier): Completable
	fun handleCommandEvent(event: SlashCommandEvent)
}

internal class CommandManagerImpl(
		resolver: Resolver
): CommandManager {
	private val jda: JDA by resolver.inject()
	private val durationParser: DurationParser by resolver.inject()
	private val commandDataParser: CommandDataParser by resolver.inject()
	private val voiceTextChannelManager: WritableVoiceTextChannelManager by resolver.inject()

	private val voiceTextCommand by lazy { VoiceTextCommand(resolver) }

	override fun setupCommandsInAllGuilds(): Completable {
		return Completable.merge(jda.guilds.map { setupCommandsInGuild(it.identifier) })
	}

	override fun setupCommandsInGuild(guild: GuildIdentifier): Completable {
		val guildEntity = requireNotNull(jda.getGuild(guild))
		return guildEntity.retrieveCommands()
				.asSingle()
				.flatMapCompletable {
					val deleteCommands = it.map { guildEntity.deleteCommand(it.identifier).asCompletable() }
					return@flatMapCompletable when (deleteCommands.size) {
						0 -> Completable.complete()
						1 -> deleteCommands.first()
						else -> Completable.merge(deleteCommands)
					}
				}
				.andThen(guildEntity.upsertCommand(commandDataParser.getCommandData(voiceTextCommand)).asSingle())
				.ignoreElement()
	}

	override fun handleCommandEvent(event: SlashCommandEvent) {
		@Suppress("UNCHECKED_CAST")
		val command = voiceTextCommand.getCommandMatchingPath(event) as? Command.Simple<Any> ?: return
		val parsedOptions = commandDataParser.parseCommandOptions(event, command.optionsKlass)
		command.handleCommand(event, parsedOptions)
	}
}